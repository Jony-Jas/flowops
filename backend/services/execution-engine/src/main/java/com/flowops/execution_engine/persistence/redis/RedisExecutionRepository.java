package com.flowops.execution_engine.persistence.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Repository for persisting execution runtime state in Redis.
 *
 * Key design:
 *  - In-degree map stored as a Redis Hash (key = indegree(flowId,runId))
 *  - Dependents stored as Redis Sets (one set per step)
 *  - Step statuses as Redis Hash (stepsHash)
 *  - Outputs in per-step Hash and mirrored into "context" for quick lookup
 *  - Ready queue as Redis List
 *  - managed_keys Set: we record every key we create in this set so clearFlow can delete only what we created
 *
 * NOTE: This class uses String values for everything. Values that are structured (outputs) are stored as JSON strings.
 */
@Repository
public class RedisExecutionRepository {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RedisExecutionRepository(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /* --------------------- Flow meta --------------------- */

    public void setFlowMeta(String flowId, String runId, Map<String, String> meta) {
        String key = RedisKeys.flowMeta(flowId, runId);
        redis.opsForHash().putAll(key, meta);
        recordKey(flowId, runId, key);
    }

    public Map<Object, Object> getFlowMeta(String flowId, String runId) {
        String key = RedisKeys.flowMeta(flowId, runId);
        return redis.opsForHash().entries(key);
    }

    /* --------------------- Step status --------------------- */

    public void setStepStatus(String flowId, String runId, String stepId, String status) {
        String key = RedisKeys.stepsHash(flowId, runId);
        redis.opsForHash().put(key, stepId, status);
        recordKey(flowId, runId, key);
    }

    public String getStepStatus(String flowId, String runId, String stepId) {
        String key = RedisKeys.stepsHash(flowId, runId);
        Object val = redis.opsForHash().get(key, stepId);
        return val == null ? null : val.toString();
    }

    public Map<String, String> getAllStepStatuses(String flowId, String runId) {
        String key = RedisKeys.stepsHash(flowId, runId);
        Map<Object, Object> entries = redis.opsForHash().entries(key);
        if (entries == null) return Collections.emptyMap();
        return entries.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
    }

    /* --------------------- Outputs & context --------------------- */

    /**
     * Save a single output value for step and also mirror into the flow-level context.
     * value is serialized to JSON.
     */
    public void setStepOutput(String flowId, String runId, String stepId, String outputKey, Object value) {
        String stepOutputsKey = RedisKeys.stepOutputs(flowId, runId, stepId);
        try {
            String json = objectMapper.writeValueAsString(value);
            redis.opsForHash().put(stepOutputsKey, outputKey, json);
            recordKey(flowId, runId, stepOutputsKey);

            // context holds "stepId.outputKey" -> json
            String ctxKey = RedisKeys.context(flowId, runId);
            String ctxField = stepId + "." + outputKey;
            redis.opsForHash().put(ctxKey, ctxField, json);
            recordKey(flowId, runId, ctxKey);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize step output", e);
        }
    }

    public Map<String, String> getStepOutputs(String flowId, String runId, String stepId) {
        String stepOutputsKey = RedisKeys.stepOutputs(flowId, runId, stepId);
        Map<Object, Object> entries = redis.opsForHash().entries(stepOutputsKey);
        if (entries == null) return Collections.emptyMap();
        return entries.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
    }

    /**
     * Returns the full execution context map: keys are "stepId.outputKey" -> JSON string value
     */
    public Map<String, String> getContext(String flowId, String runId) {
        String ctxKey = RedisKeys.context(flowId, runId);
        Map<Object, Object> ids = redis.opsForHash().entries(ctxKey);
        if (ids == null) return Collections.emptyMap();
        return ids.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
    }

    /* --------------------- DAG: indegree & dependents --------------------- */

    /**
     * Store the indegree map (stepId -> count) as strings.
     */
    public void setIndegreeMap(String flowId, String runId, Map<String, Integer> indegree) {
        String key = RedisKeys.indegree(flowId, runId);
        Map<String, String> asStrings = indegree.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));
        redis.opsForHash().putAll(key, asStrings);
        recordKey(flowId, runId, key);
    }

    /**
     * Atomically decrement in-degree for a single step and return the new value.
     * Uses HINCRBY under the hood (increment with -1).
     *
     * @return the updated indegree (>= 0)
     */
    public long decrementIndegreeAndGet(String flowId, String runId, String stepId) {
        String key = RedisKeys.indegree(flowId, runId);
        // using Number return from opsForHash().increment
        Long updated = redis.opsForHash().increment(key, stepId, -1L);
        recordKey(flowId, runId, key);
        return updated == null ? -1 : updated.longValue();
    }

    public Map<String, Integer> getIndegreeMap(String flowId, String runId) {
        String key = RedisKeys.indegree(flowId, runId);
        Map<Object, Object> m = redis.opsForHash().entries(key);
        if (m == null) return Collections.emptyMap();
        return m.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), e -> Integer.parseInt(e.getValue().toString())));
    }

    public void addDependent(String flowId, String runId, String stepId, String dependentStepId) {
        String key = RedisKeys.dependents(flowId, runId, stepId);
        redis.opsForSet().add(key, dependentStepId);
        recordKey(flowId, runId, key);
    }

    public Set<String> getDependents(String flowId, String runId, String stepId) {
        String key = RedisKeys.dependents(flowId, runId, stepId);
        Set<String> members = redis.opsForSet().members(key);
        return members == null ? Collections.emptySet() : members;
    }

    /* --------------------- Ready queue --------------------- */

    public void enqueueReadyStep(String flowId, String runId, String stepId) {
        String key = RedisKeys.readyQueue(flowId, runId);
        redis.opsForList().rightPush(key, stepId);
        recordKey(flowId, runId, key);
    }

    /**
     * Non-blocking pop. Returns null if empty.
     */
    public String dequeueReadyStep(String flowId, String runId) {
        String key = RedisKeys.readyQueue(flowId, runId);
        return redis.opsForList().leftPop(key);
    }

    /**
     * Blocking pop with timeout (seconds). Useful if you want to wait for a ready step.
     */
    public String blockingDequeueReadyStep(String flowId, String runId, long timeoutSeconds) {
        String key = RedisKeys.readyQueue(flowId, runId);
        return redis.opsForList().leftPop(key, timeoutSeconds, TimeUnit.SECONDS);
    }

    /* --------------------- Managed keys bookkeeping & clear --------------------- */

    /**
     * Record a key into the managed keys set for this flow - used for safe deletion later.
     */
    private void recordKey(String flowId, String runId, String key) {
        String managed = RedisKeys.managedKeysSet(flowId, runId);
        redis.opsForSet().add(managed, key);
        // set a TTL for the bookkeeping set to avoid orphan managed sets if flow never cleared
        redis.expire(managed, 7, TimeUnit.DAYS);
    }

    /**
     * Delete all keys created for the flow/run. Returns the number of keys removed.
     */
    public long clearFlow(String flowId, String runId) {
        String managedKeySet = RedisKeys.managedKeysSet(flowId, runId);
        Set<String> keys = redis.opsForSet().members(managedKeySet);
        long deleted = 0;
        if (keys != null && !keys.isEmpty()) {
            // convert to array and delete
            String[] arr = keys.toArray(new String[0]);
            redis.delete(Arrays.asList(arr));
            // delete the managed set itself
            redis.delete(managedKeySet);
            deleted = arr.length + 1;
        }
        // also attempt to delete well-known keys in case managed set wasn't created
        List<String> fallbacks = List.of(
                RedisKeys.flowMeta(flowId, runId),
                RedisKeys.stepsHash(flowId, runId),
                RedisKeys.context(flowId, runId),
                RedisKeys.indegree(flowId, runId),
                RedisKeys.readyQueue(flowId, runId)
        );
        deleted += redis.delete(fallbacks);
        return deleted;
    }

    /* --------------------- Utility / helper read methods --------------------- */

    /**
     * Convenience: returns a snapshot of the run state for debugging.
     */
    public Map<String, Object> snapshot(String flowId, String runId) {
        Map<String, Object> snap = new HashMap<>();
        snap.put("meta", getFlowMeta(flowId, runId));
        snap.put("steps", getAllStepStatuses(flowId, runId));
        snap.put("indegree", getIndegreeMap(flowId, runId));
        snap.put("context", getContext(flowId, runId));
        return snap;
    }

    /* --------------------- Flow definition persistence --------------------- */

    /**
     * Persist flattened flow definition JSON for crash recovery.
     * Stored as a plain string value at key flow:{flowId}:{runId}:def
     */
    public void setFlowDefinition(String flowId, String runId, String json) {
        String key = RedisKeys.flowDefinition(flowId, runId);
        redis.opsForValue().set(key, json);
        // record this key in the managed-keys set for this flow so clearFlow will delete it later
        recordKey(flowId, runId, key);
    }

    /**
     * Retrieve the persisted flow definition JSON if present.
     */
    public Optional<String> getFlowDefinition(String flowId, String runId) {
        String key = RedisKeys.flowDefinition(flowId, runId);
        String val = redis.opsForValue().get(key);
        return Optional.ofNullable(val);
    }

    /* --------------------- Active runs index (global set) --------------------- */

    /**
     * Add runKey (format "flowId:runId") to the global active runs set.
     * This is intentionally NOT recorded into per-flow managed-keys because it's a global index.
     */
    public void addActiveRun(String runKey) {
        String activeKey = RedisKeys.activeRunsKey();
        redis.opsForSet().add(activeKey, runKey);
    }

    /**
     * Remove runKey from the global active runs set.
     */
    public void removeActiveRun(String runKey) {
        String activeKey = RedisKeys.activeRunsKey();
        redis.opsForSet().remove(activeKey, runKey);
    }

    /**
     * List all active run keys (each entry is "flowId:runId").
     */
    public List<String> listActiveRunKeys() {
        String activeKey = RedisKeys.activeRunsKey();
        Set<String> members = redis.opsForSet().members(activeKey);
        if (members == null || members.isEmpty()) return Collections.emptyList();
        // return as a list (stable order not guaranteed)
        return members.stream().collect(Collectors.toList());
    }
}
