package com.flowops.kafka_contracts.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionCommandEvent {

    private EventType eventType;   // START / STOP / PAUSE / RESUME
    private String flowId;
    private String runId;
    private String triggeredBy;
    private Instant timestamp;

    // Only populated when eventType == EXECUTION_START
    private List<Step> steps;

    public enum EventType {
        EXECUTION_START,
        EXECUTION_STOP,
        EXECUTION_PAUSE,
        EXECUTION_RESUME
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Step {
        private String stepId;
        private String pluginId;
        private StepType type;
        private Map<String, String> inputs;
        private Map<String, String> outputs;
        private String condition;
        private List<Branch> branches;
        private List<Step> children;
        private Config config;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Branch {
        private String branchId;
        private String condition;
        private List<Step> steps;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Config {
        private int timeoutMs;
        private int retryCount;
        private int retryDelayMs;
    }

    public enum StepType {
        ACTION, DECISION
    }
}
