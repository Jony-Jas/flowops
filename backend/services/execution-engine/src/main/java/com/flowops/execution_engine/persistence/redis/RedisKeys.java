package com.flowops.execution_engine.persistence.redis;

public final class RedisKeys {
    private RedisKeys() {}

    public static String flowMeta(String flowId, String runId) {
        return String.format("flow:%s:%s:meta", flowId, runId);
    }

    public static String stepsHash(String flowId, String runId) {
        return String.format("flow:%s:%s:steps", flowId, runId);
    }

    public static String stepOutputs(String flowId, String runId, String stepId) {
        return String.format("flow:%s:%s:step:%s:outputs", flowId, runId, stepId);
    }

    public static String context(String flowId, String runId) {
        return String.format("flow:%s:%s:context", flowId, runId);
    }

    public static String indegree(String flowId, String runId) {
        return String.format("flow:%s:%s:dag:indegree", flowId, runId);
    }

    public static String dependents(String flowId, String runId, String stepId) {
        return String.format("flow:%s:%s:dag:%s:dependents", flowId, runId, stepId);
    }

    public static String readyQueue(String flowId, String runId) {
        return String.format("flow:%s:%s:queue", flowId, runId);
    }

    /**
     * A private bookkeeping set that contains the list of keys the repository created for this flow/run.
     * Using this we can safely delete only keys we created (safer than KEYS pattern).
     */
    public static String managedKeysSet(String flowId, String runId) {
        return String.format("flow:%s:%s:managed_keys", flowId, runId);
    }

    /**
     * Key for storing serialized (JSON) flattened flow definition.
     * e.g. flow:{flowId}:{runId}:def
     */
    public static String flowDefinition(String flowId, String runId) {
        return String.format("flow:%s:%s:def", flowId, runId);
    }

    /**
     * Global set that lists active runs. Values are stored as "flowId:runId".
     * Key example: flowops:active_runs
     */
    public static String activeRunsKey() {
        return "flowops:active_runs";
    }
}
