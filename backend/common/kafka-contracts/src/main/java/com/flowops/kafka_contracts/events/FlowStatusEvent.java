package com.flowops.kafka_contracts.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Flow-level status update published by execution engine.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowStatusEvent {

    private EventType eventType;   // FLOW_STATUS
    private String flowId;
    private String runId;
    private Status status;         // PENDING, RUNNING, etc.
    private String error;          // optional top-level error message
    private Instant timestamp;

    public enum EventType {
        FLOW_STATUS
    }

    public enum Status {
        PENDING,
        RUNNING,
        PAUSED,
        COMPLETED,
        FAILED,
        STOPPED
    }
}
