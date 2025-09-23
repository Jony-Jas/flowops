package com.flowops.kafka_contracts.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Command event sent to execution engine to control lifecycle.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionCommandEvent {

    private EventType eventType;   // EXECUTION_START | EXECUTION_STOP | EXECUTION_PAUSE | EXECUTION_RESUME
    private String flowId;
    private String runId;
    private String triggeredBy;
    private Instant timestamp;

    public enum EventType {
        EXECUTION_START,
        EXECUTION_STOP,
        EXECUTION_PAUSE,
        EXECUTION_RESUME
    }
}