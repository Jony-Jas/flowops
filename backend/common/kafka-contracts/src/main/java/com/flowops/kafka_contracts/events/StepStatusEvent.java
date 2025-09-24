package com.flowops.kafka_contracts.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Step-level status update published by execution engine.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepStatusEvent {

    private EventType eventType;   // STEP_STATUS
    private String flowId;
    private String runId;
    private String stepId;
    private Status status;         // PENDING, RUNNING, COMPLETED, etc.
    private String error;          // optional step error
    private Instant timestamp;

    public enum EventType {
        STEP_STATUS
    }

    public enum Status {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        SKIPPED,
        CANCELLED
    }
}