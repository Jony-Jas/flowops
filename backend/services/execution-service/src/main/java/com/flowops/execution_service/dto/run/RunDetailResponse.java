package com.flowops.execution_service.dto.run;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class RunDetailResponse {
    private UUID id;
    private UUID flowId;
    private String triggeredBy;
    private String status;
    private String error;
    private Instant startedAt;
    private Instant completedAt;
    private Map<String, Object> outputs;
    private List<RunStepDto> steps;

    @Data
    @Builder
    public static class RunStepDto {
        private UUID stepId;
        private String status;
        private String error;
        private Instant startedAt;
        private Instant completedAt;
    }
}
