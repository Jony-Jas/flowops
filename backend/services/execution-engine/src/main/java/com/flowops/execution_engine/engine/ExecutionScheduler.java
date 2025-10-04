package com.flowops.execution_engine.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowops.execution_engine.model.Step;
import com.flowops.kafka_contracts.events.ExecutionCommandEvent;
import com.flowops.kafka_contracts.events.FlowStatusEvent;
import com.flowops.kafka_contracts.events.StepStatusEvent;
import com.flowops.execution_engine.kafka.StatusEventProducer;
import com.flowops.execution_engine.persistence.redis.RedisExecutionRepository;
import com.flowops.execution_engine.executor.StepExecutor;
import com.flowops.execution_engine.grpc.PluginServiceClient;
import com.flowops.execution_engine.mapper.ExecutionEventMapper;
import com.flowops.common.grpc.PluginMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * ExecutionScheduler — improved lifecycle handling + recovery.
 *
 * Key differences:
 *  - Persist flow definition JSON at START (repo.setFlowDefinition)
 *  - Maintain active runs index (repo.addActiveRun/listActiveRunKeys/removeActiveRun)
 *  - Use StepExecutor.createExecutionCallable(...) so that the scheduler submits a Callable
 *    to taskExecutor (so Future.cancel(true) interrupts the worker thread)
 *  - On startup recover runs from Redis and rebuild FlowRunControl
 */
@Component
public class ExecutionScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExecutionScheduler.class);

    private final RedisExecutionRepository repo;
    private final DAGBuilder dagBuilder;
    private final DAGValidator dagValidator;
    private final StepExecutor stepExecutor;
    private final StatusEventProducer producer;
    private final ExecutionEventMapper eventMapper;
    private final PluginServiceClient pluginClient; // used to fetch metadata for caching & validation

    private final ObjectMapper om = new ObjectMapper();

    // worker pool for executing steps (submit Callables returned by StepExecutor)
    private final ExecutorService taskExecutor;

    // thread pool for dispatchers (one dispatcher per active run)
    private final ExecutorService dispatcherExecutor;

    // in-memory active run controls
    private final ConcurrentMap<String, FlowRunControl> runs = new ConcurrentHashMap<>();

    public ExecutionScheduler(RedisExecutionRepository repo,
                              DAGBuilder dagBuilder,
                              DAGValidator dagValidator,
                              StepExecutor stepExecutor,
                              StatusEventProducer producer,
                              ExecutionEventMapper eventMapper,
                              PluginServiceClient pluginClient) {
        this.repo = repo;
        this.dagBuilder = dagBuilder;
        this.dagValidator = dagValidator;
        this.stepExecutor = stepExecutor;
        this.producer = producer;
        this.pluginClient = pluginClient;
        this.eventMapper = eventMapper;

        // tuned thread pools; adjust to your infra
        this.taskExecutor = Executors.newFixedThreadPool(Math.max(8, Runtime.getRuntime().availableProcessors() * 2));
        this.dispatcherExecutor = Executors.newCachedThreadPool();
    }

    /* -------------------- PUBLIC LIFECYCLE APIs -------------------- */

    public void startFlow(ExecutionCommandEvent cmd) {
        Objects.requireNonNull(cmd);
        final String flowId = cmd.getFlowId();
        final String runId = cmd.getRunId();
        final String runKey = runKey(flowId, runId);

        if (runs.containsKey(runKey)) {
            log.warn("startFlow: run already active {}:{}", flowId, runId);
            return;
        }

        Map<String, Step> flat;
        try {
            List<Step> domainSteps = eventMapper.toDomainList(cmd.getSteps());
            flat = dagBuilder.flattenSteps(domainSteps);
        } catch (Exception ex) {
            log.error("startFlow: flatten error {}:{}", flowId, runId, ex.getMessage());
            producer.sendFlowStatus(flowId, runId, FlowStatusEvent.Status.FAILED, "Invalid flow definition: " + ex.getMessage());
            return;
        }

        DAGBuilder.DAG dag;
        try {
            dag = dagBuilder.buildDAG(flat);
            dagValidator.validateNoCycles(dag);
        } catch (Exception ex) {
            log.error("startFlow: DAG invalid {}:{}", flowId, runId, ex.getMessage());
            producer.sendFlowStatus(flowId, runId, FlowStatusEvent.Status.FAILED, "DAG invalid: " + ex.getMessage());
            return;
        }

        // Persist the flattened flow definition for crash-recovery. Store as JSON.
        try {
            String defJson = om.writeValueAsString(flat);
            repo.setFlowDefinition(flowId, runId, defJson);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize flow definition to Redis for {}:{}. Continuing without persistence.", flowId, runId, e);
        }

        // Persist DAG indegree and dependents
        repo.setIndegreeMap(flowId, runId, dag.getIndegree());
        dag.getAdjacency().forEach((dep, deps) -> {
            for (String d : deps) repo.addDependent(flowId, runId, dep, d);
        });

        // init step statuses
        dag.getIndegree().keySet().forEach(stepId -> repo.setStepStatus(flowId, runId, stepId, StepStatusEvent.Status.PENDING.name()));

        // seed ready queue
        dag.getIndegree().forEach((stepId, indeg) -> { if (indeg == 0) repo.enqueueReadyStep(flowId, runId, stepId); });

        // set flow meta & mark active
        repo.setFlowMeta(flowId, runId, Map.of("status", FlowStatusEvent.Status.RUNNING.name(), "startedAt", Instant.now().toString()));
        repo.addActiveRun(runKey);

        producer.sendFlowStatus(flowId, runId, FlowStatusEvent.Status.RUNNING, null);

        // create control and start dispatcher
        FlowRunControl ctrl = new FlowRunControl(flowId, runId, flat, dag);
        runs.put(runKey, ctrl);
        startDispatcher(ctrl);
        log.info("Started run {}:{}", flowId, runId);
    }

    public void pauseFlow(String flowId, String runId) {
        String rk = runKey(flowId, runId);
        FlowRunControl c = runs.get(rk);
        if (c == null) {
            log.warn("pauseFlow: run not in memory {}:{}. Marking PAUSED in redis for recovery.", flowId, runId);
            repo.setFlowMeta(flowId, runId, Map.of("status", FlowStatusEvent.Status.PAUSED.name(), "pausedAt", Instant.now().toString()));
            // In a crash-recovery scenario, resume will rebuild control and honor paused state.
            producer.sendFlowStatus(flowId, runId, FlowStatusEvent.Status.PAUSED, null);
            return;
        }
        c.setPaused(true);
        repo.setFlowMeta(flowId, runId, Map.of("status", FlowStatusEvent.Status.PAUSED.name(), "pausedAt", Instant.now().toString()));
        producer.sendFlowStatus(flowId, runId, FlowStatusEvent.Status.PAUSED, null);
        log.info("Paused run {}:{}", flowId, runId);
    }

    public void resumeFlow(String flowId, String runId) {
        String rk = runKey(flowId, runId);
        FlowRunControl c = runs.get(rk);
        if (c != null) {
            c.setPaused(false);
            repo.setFlowMeta(flowId, runId, Map.of("status", FlowStatusEvent.Status.RUNNING.name(), "resumedAt", Instant.now().toString()));
            producer.sendFlowStatus(flowId, runId, FlowStatusEvent.Status.RUNNING, null);
            log.info("Resumed run {}:{}", flowId, runId);
            return;
        }

        // If run is not in memory -> attempt to rebuild from persisted flow definition (CRASH RECOVERY)
        Optional<String> optDef = repo.getFlowDefinition(flowId, runId);
        if (optDef.isPresent()) {
            try {
                // rebuild flattened map and dag
                Map<String, Step> flat = om.readValue(optDef.get(), om.getTypeFactory().constructMapType(Map.class, String.class, Step.class));
                DAGBuilder.DAG dag = dagBuilder.buildDAG(flat);
                // Reconstruct FlowRunControl and re-seed any ready steps from Redis indegree/queue
                FlowRunControl ctrl = new FlowRunControl(flowId, runId, flat, dag);
                runs.put(rk, ctrl);
                ctrl.setPaused(false);
                repo.setFlowMeta(flowId, runId, Map.of("status", FlowStatusEvent.Status.RUNNING.name(), "resumedAt", Instant.now().toString()));
                producer.sendFlowStatus(flowId, runId, FlowStatusEvent.Status.RUNNING, null);
                startDispatcher(ctrl);
                log.info("Recovered and resumed run {}:{}", flowId, runId);
            } catch (Exception ex) {
                log.error("Failed to rebuild run {}:{} for resume: {}", flowId, runId, ex.getMessage(), ex);
                producer.sendFlowStatus(flowId, runId, FlowStatusEvent.Status.FAILED, "resume failed: " + ex.getMessage());
            }
        } else {
            log.warn("No persisted flow definition for {}:{}. Cannot resume.", flowId, runId);
        }
    }

    public void stopFlow(String flowId, String runId) {
        String rk = runKey(flowId, runId);
        FlowRunControl ctrl = runs.remove(rk);
        if (ctrl != null) {
            ctrl.setRunning(false);
            ctrl.cancelAllRunningTasks(); // cancels Future.s and interrupts worker threads
            long removed = repo.clearFlow(flowId, runId);
            repo.removeActiveRun(rk);
            producer.sendFlowStatus(flowId, runId, FlowStatusEvent.Status.STOPPED, null);
            log.info("Stopped run {}:{} removed {} keys", flowId, runId, removed);
            return;
        }
        // if not in memory, still clear persisted state
        long removed = repo.clearFlow(flowId, runId);
        repo.removeActiveRun(rk);
        producer.sendFlowStatus(flowId, runId, FlowStatusEvent.Status.STOPPED, null);
        log.info("Stopped (not-in-memory) run {}:{} removed {} keys", flowId, runId, removed);
    }

    /* -------------------- Dispatcher & scheduling -------------------- */

    private void startDispatcher(FlowRunControl ctrl) {
        dispatcherExecutor.submit(() -> {
            try {
                dispatcherLoop(ctrl);
            } catch (Throwable t) {
                log.error("Dispatcher for {}:{} crashed: {}", ctrl.flowId, ctrl.runId, t.getMessage(), t);
                producer.sendFlowStatus(ctrl.flowId, ctrl.runId, FlowStatusEvent.Status.FAILED, t.getMessage());
                ctrl.setRunning(false);
            }
        });
    }

    private void dispatcherLoop(FlowRunControl ctrl) throws InterruptedException {
        ctrl.setRunning(true);
        log.info("Dispatcher started for {}:{}", ctrl.flowId, ctrl.runId);

        while (ctrl.isRunning()) {
            if (ctrl.isPaused()) {
                Thread.sleep(250);
                continue;
            }

            // blocking pop with timeout for periodic checks
            String nextStep = repo.blockingDequeueReadyStep(ctrl.flowId, ctrl.runId, 1);
            if (nextStep == null) {
                if (isFlowCompleted(ctrl)) {
                    // mark completed and cleanup
                    producer.sendFlowStatus(ctrl.flowId, ctrl.runId, FlowStatusEvent.Status.COMPLETED, null);
                    repo.clearFlow(ctrl.flowId, ctrl.runId);
                    repo.removeActiveRun(runKey(ctrl.flowId, ctrl.runId));
                    ctrl.setRunning(false);
                    runs.remove(runKey(ctrl.flowId, ctrl.runId));
                    log.info("Flow completed {}:{}", ctrl.flowId, ctrl.runId);
                    break;
                }
                continue;
            }

            // check paused after pop
            if (ctrl.isPaused()) {
                repo.enqueueReadyStep(ctrl.flowId, ctrl.runId, nextStep);
                Thread.sleep(250);
                continue;
            }

            // dispatch step
            dispatchStep(ctrl, nextStep);
        }
        log.info("Dispatcher exiting for {}:{}", ctrl.flowId, ctrl.runId);
    }

    private void dispatchStep(FlowRunControl ctrl, String stepId) {
        String flowId = ctrl.flowId;
        String runId = ctrl.runId;

        // set RUNNING status
        repo.setStepStatus(flowId, runId, stepId, StepStatusEvent.Status.RUNNING.name());
        producer.sendStepStatus(flowId, runId, stepId, StepStatusEvent.Status.RUNNING, null);

        // fetch plugin metadata (used by StepExecutor to coerce inputs & outputs)
        Step step = ctrl.flatSteps.get(stepId);
        PluginMetadata metadata = null;
        try {
            metadata = pluginClient.getMetadata(step.getPluginId(), null);
        } catch (Exception ex) {
            log.warn("Failed to fetch plugin metadata for step {}: {}. Proceeding assuming plugin metadata unavailable.", stepId, ex.getMessage());
        }

        // get a blocking Callable from StepExecutor (does not spawn its own thread)
        Callable<Map<String, Object>> callable = stepExecutor.createExecutionCallable(step, flowId, runId, metadata);

        // submit to taskExecutor
        Future<Map<String, Object>> future = taskExecutor.submit(callable);

        // register future in control
        ctrl.addRunningFuture(stepId, future);

        // asynchronously handle completion
        taskExecutor.submit(() -> {
            try {
                Map<String, Object> outputs = future.get(); // will block until complete or throw CancellationException/ExecutionException
                if (outputs != null) {
                    outputs.forEach((k, v) -> repo.setStepOutput(flowId, runId, stepId, k, v));
                }
                repo.setStepStatus(flowId, runId, stepId, StepStatusEvent.Status.COMPLETED.name());
                producer.sendStepStatus(flowId, runId, stepId, StepStatusEvent.Status.COMPLETED, null);

                // update dependents
                Set<String> dependents = repo.getDependents(flowId, runId, stepId);
                for (String dep : dependents) {
                    long newInd = repo.decrementIndegreeAndGet(flowId, runId, dep);
                    if (newInd == 0) repo.enqueueReadyStep(flowId, runId, dep);
                }
            } catch (CancellationException ce) {
                // step was cancelled (STOP); mark CANCELLED
                repo.setStepStatus(flowId, runId, stepId, StepStatusEvent.Status.CANCELLED.name());
                producer.sendStepStatus(flowId, runId, stepId, StepStatusEvent.Status.CANCELLED, "Cancelled");
            } catch (ExecutionException ee) {
                Throwable cause = ee.getCause();
                String err = cause == null ? ee.getMessage() : cause.getMessage();
                repo.setStepStatus(flowId, runId, stepId, StepStatusEvent.Status.FAILED.name());
                producer.sendStepStatus(flowId, runId, stepId, StepStatusEvent.Status.FAILED, err);
                producer.sendFlowStatus(flowId, runId, FlowStatusEvent.Status.FAILED, err);
                // stop scheduling further steps in this run
                ctrl.setRunning(false);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } finally {
                ctrl.removeRunningFuture(stepId);
            }
        });
    }

    private boolean isFlowCompleted(FlowRunControl ctrl) {
        Map<String, String> statuses = repo.getAllStepStatuses(ctrl.flowId, ctrl.runId);
        if (statuses == null || statuses.isEmpty()) return false;
        for (String val : statuses.values()) {
            if (!StepStatusEvent.Status.COMPLETED.name().equals(val) &&
                !StepStatusEvent.Status.SKIPPED.name().equals(val)) {
                return false;
            }
        }
        return true;
    }

    /* -------------------- Recovery on startup -------------------- */

    @PostConstruct
    public void recoverRunsOnStartup() {
        log.info("Recovering active runs from Redis...");
        List<String> runKeys = repo.listActiveRunKeys(); // e.g., ["flow1:runA", "flow2:runB"]
        if (runKeys == null || runKeys.isEmpty()) { log.info("No active runs found."); return; }

        for (String rk : runKeys) {
            String[] parts = rk.split(":", 2);
            if (parts.length != 2) {
                log.warn("Invalid runKey in active set: {}", rk);
                continue;
            }
            String flowId = parts[0], runId = parts[1];
            Optional<String> defOpt = repo.getFlowDefinition(flowId, runId);
            if (defOpt.isEmpty()) {
                log.warn("No flow definition for {}:{}. Skipping recovery.", flowId, runId);
                continue;
            }
            try {
                Map<String, Step> flat = om.readValue(defOpt.get(), om.getTypeFactory().constructMapType(Map.class, String.class, Step.class));
                DAGBuilder.DAG dag = dagBuilder.buildDAG(flat);
                FlowRunControl ctrl = new FlowRunControl(flowId, runId, flat, dag);
                runs.put(rk, ctrl);
                // if flow was paused in redis, keep paused state
                Map<Object, Object> meta = repo.getFlowMeta(flowId, runId);
                if (meta != null && "PAUSED".equalsIgnoreCase(String.valueOf(meta.get("status")))) {
                    ctrl.setPaused(true);
                    log.info("Recovered run {}:{} in PAUSED state.", flowId, runId);
                }
                startDispatcher(ctrl);
                log.info("Recovered dispatcher for {}:{}", flowId, runId);
            } catch (Exception ex) {
                log.error("Failed to recover run {}:{} - {}", flowId, runId, ex.getMessage(), ex);
            }
        }
    }

    /* -------------------- Graceful JVM shutdown -------------------- */

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down ExecutionScheduler — stopping runs and cancelling tasks...");
        // stop all runs
        for (String rk : new ArrayList<>(runs.keySet())) {
            FlowRunControl c = runs.remove(rk);
            if (c != null) {
                c.setRunning(false);
                c.cancelAllRunningTasks();
                repo.setFlowMeta(c.flowId, c.runId, Map.of("status", FlowStatusEvent.Status.PAUSED.name(), "pausedAt", Instant.now().toString()));
                producer.sendFlowStatus(c.flowId, c.runId, FlowStatusEvent.Status.PAUSED, "Engine shutting down");
            }
        }
        try {
            taskExecutor.shutdownNow();
            dispatcherExecutor.shutdownNow();
        } catch (Exception ignored) {}
    }

    /* -------------------- Helpers & FlowRunControl -------------------- */

    private static class FlowRunControl {
        final String flowId;
        final String runId;
        final Map<String, Step> flatSteps;
        final DAGBuilder.DAG dag;

        private volatile boolean running = false;
        private volatile boolean paused = false;
        // track stepId -> Future returned by taskExecutor for cancellation
        private final ConcurrentMap<String, Future<?>> runningFutures = new ConcurrentHashMap<>();

        FlowRunControl(String flowId, String runId, Map<String, Step> flatSteps, DAGBuilder.DAG dag) {
            this.flowId = flowId;
            this.runId = runId;
            this.flatSteps = flatSteps;
            this.dag = dag;
        }

        void addRunningFuture(String stepId, Future<?> f) { runningFutures.put(stepId, f); }
        void removeRunningFuture(String stepId) { runningFutures.remove(stepId); }
        void cancelAllRunningTasks() {
            for (Map.Entry<String, Future<?>> e : runningFutures.entrySet()) {
                try {
                    e.getValue().cancel(true); // interrupt worker thread executing the plugin callable
                } catch (Exception ignored) { }
            }
            runningFutures.clear();
        }
        boolean isRunning() { return running; }
        void setRunning(boolean v) { running = v; }
        boolean isPaused() { return paused; }
        void setPaused(boolean v) { paused = v; }
    }

    private String runKey(String flowId, String runId) { return flowId + ":" + runId; }
}
