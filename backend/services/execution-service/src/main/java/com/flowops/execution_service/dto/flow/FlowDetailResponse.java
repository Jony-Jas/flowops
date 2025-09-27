package com.flowops.execution_service.dto.flow;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class FlowDetailResponse {
    private UUID id;
    private String name;
    private String description;
    private List<StepDto> steps;
    private String cron;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    public static class StepDto {
        private UUID stepId;
        private UUID pluginId;
        private String type;
        private Map<String, String> inputs;
        private Map<String, String> outputs;
        private String condition;
        private List<BranchDto> branches;
        private List<StepDto> children;
    }

    @Data
    @Builder
    public static class BranchDto {
        private UUID branchId;
        private String condition;
        private List<StepDto> steps;
    }
}
