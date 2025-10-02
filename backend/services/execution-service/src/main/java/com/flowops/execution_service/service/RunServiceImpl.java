package com.flowops.execution_service.service;

import com.flowops.execution_service.dto.run.RunDetailResponse;
import com.flowops.execution_service.dto.run.RunRequest;
import com.flowops.execution_service.exception.NotFoundException;
import com.flowops.execution_service.mapper.ExecutionCommandMapper;
import com.flowops.execution_service.mapper.RunMapper;
import com.flowops.execution_service.model.Flow;
import com.flowops.execution_service.model.Run;
import com.flowops.execution_service.repository.FlowRepository;
import com.flowops.execution_service.repository.RunRepository;
import com.flowops.kafka_contracts.Topics;
import com.flowops.kafka_contracts.events.ExecutionCommandEvent;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RunServiceImpl implements RunService {

    private final RunRepository runRepository;
    private final FlowRepository flowRepository;
    private final RunMapper runMapper;
    private final EventPublisher eventPublisher;
    private final ExecutionCommandMapper commandMapper;

    @Override
    public UUID startRun(UUID flowId, RunRequest request) {
        Flow flow = flowRepository.findById(flowId)
                .orElseThrow(() -> new NotFoundException("Flow not found: " + flowId));

        // Flatten the DAG into RunSteps
        List<Run.RunStep> runSteps = flattenSteps(flow.getSteps());

        Run run = Run.builder()
                .id(UUID.randomUUID())
                .flowId(flowId)
                .triggeredBy(request.getTriggeredBy())
                .status(Run.RunStatus.PENDING)
                .steps(runSteps) // flattened list of all steps
                .createdAt(Instant.now())
                .build();
        runRepository.save(run);

        // Send START command with full flow definition
        ExecutionCommandEvent event = ExecutionCommandEvent.builder()
                .eventType(ExecutionCommandEvent.EventType.EXECUTION_START)
                .flowId(flowId.toString())
                .runId(run.getId().toString())
                .triggeredBy(request.getTriggeredBy())
                .timestamp(Instant.now())
                .steps(flow.getSteps().stream()
                        .map(commandMapper::toCommandStep)
                        .toList())
                .build();

        eventPublisher.publish(Topics.EXECUTION_COMMANDS, event, run.getId().toString());

        return run.getId();
    }

    @Override
    public void pauseRun(UUID runId) {
        updateStatus(runId, Run.RunStatus.PAUSED);
    }

    @Override
    public void resumeRun(UUID runId) {
        updateStatus(runId, Run.RunStatus.RUNNING);
    }

    @Override
    public void stopRun(UUID runId) {
        updateStatus(runId, Run.RunStatus.STOPPED);
    }

    @Override
    public RunDetailResponse getRunStatus(UUID runId) {
        Run run = runRepository.findById(runId)
                .orElseThrow(() -> new NotFoundException("Run not found: " + runId));
        return runMapper.toDetailResponse(run);
    }

    private void updateStatus(UUID runId, Run.RunStatus status) {
        Run run = runRepository.findById(runId)
                .orElseThrow(() -> new NotFoundException("Run not found: " + runId));
        run.setStatus(status);
        run.setUpdatedAt(Instant.now());
        runRepository.save(run);
    }

    private List<Run.RunStep> flattenSteps(List<Flow.Step> steps) {
        List<Run.RunStep> runSteps = new ArrayList<>();
        if (steps == null) return runSteps;

        for (Flow.Step step : steps) {
            // Add this step
            runSteps.add(Run.RunStep.builder()
                    .stepId(step.getStepId())
                    .status(Run.StepStatus.PENDING)
                    .build());

            // Add children recursively
            runSteps.addAll(flattenSteps(step.getChildren()));

            // Add branches recursively
            if (step.getBranches() != null) {
                for (Flow.Branch branch : step.getBranches()) {
                    runSteps.addAll(flattenSteps(branch.getSteps()));
                }
            }
        }
        return runSteps;
    }

}
