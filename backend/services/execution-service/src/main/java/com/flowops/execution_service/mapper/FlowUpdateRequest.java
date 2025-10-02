package com.flowops.execution_service.mapper;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class FlowUpdateRequest {

    @NotBlank
    private String name;

    private String description;

    private String cron;

    private List<StepDto> steps;

    @Data
    public static class StepDto {
        private UUID stepId;
        private UUID pluginId;
        private String type; // "ACTION" | "DECISION"
        private Map<String, String> inputs;
        private Map<String, String> outputs;
        private String condition;
        private List<BranchDto> branches;
        private List<StepDto> children;
        private ConfigDto config;
    }

    @Data
    public static class BranchDto {
        private UUID branchId;
        private String condition;
        private List<StepDto> steps;
    }

    @Data
    public static class ConfigDto {
        private int timeoutMs;
        private int retryCount;
        private int retryDelayMs;
    }
}
