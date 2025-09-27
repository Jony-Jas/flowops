package com.flowops.execution_service.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "runs")
public class Run {

    @Id
    private UUID id;                       // runId

    private UUID flowId;                   // reference to flow definition
    private String triggeredBy;            // uuid | system
    private RunStatus status;

    private List<RunStep> steps;           // execution steps
    private Map<String, Object> outputs;   // aggregated outputs
    private String error;                  // top-level error

    private Instant startedAt;
    private Instant completedAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    // --- Nested Classes ---

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RunStep {
        private UUID stepId;               // reference to step in definition
        private StepStatus status;
        private String error;              // short error message
        private Instant startedAt;
        private Instant completedAt;
    }

    public enum RunStatus {
        PENDING,
        RUNNING,
        PAUSED,
        COMPLETED,
        FAILED,
        STOPPED,
        SCHEDULED
    }

    public enum StepStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        SKIPPED,
        CANCELLED
    }
}