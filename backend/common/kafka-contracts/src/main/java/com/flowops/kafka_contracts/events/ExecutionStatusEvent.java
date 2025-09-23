package com.flowops.kafka_contracts.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Status update published by execution engine and consumed by execution-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionStatusEvent {

    private EventType eventType;   // EXECUTION_STATUS
    private String flowId;
    private String runId;
    private Status status;         // RUNNING, COMPLETED, etc.
    private List<StepUpdate> stepUpdates;
    private String error;          // optional top-level error
    private Instant timestamp;

    public enum EventType {
        EXECUTION_STATUS
    }

    public enum Status {
        PENDING,
        RUNNING,
        PAUSED,
        COMPLETED,
        FAILED,
        STOPPED
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepUpdate {
        private String stepId;
        private StepStatus status;
        private String error;

        public enum StepStatus {
            PENDING,
            RUNNING,
            COMPLETED,
            FAILED,
            SKIPPED,
            CANCELLED
        }
    }
}