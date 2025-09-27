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
@Document(collection = "flows")
public class Flow {

    @Id
    private UUID id;                         // flowId

    private String name;
    private String description;

    private List<Step> steps;                // DAG definition
    private Schedule schedule;               // Only for scheduled/cron executions
    private UUID ownerId;                    // Link to user

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    // --- Nested Classes ---

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Step {
        private UUID stepId;
        private UUID pluginId;
        private StepType type;                   // action | decision
        private Map<String, String> inputs;      // inputKey → static_value/depStepId.outputKey
        private Map<String, String> outputs;     // outputKey → type
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
        private UUID branchId;
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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Schedule {
        private ScheduleType type;   // IMMEDIATE | CRON
        private String cron;
        private Instant nextRunAt;
    }

    public enum StepType {
        ACTION, DECISION
    }

    public enum ScheduleType {
        IMMEDIATE, CRON
    }
}
