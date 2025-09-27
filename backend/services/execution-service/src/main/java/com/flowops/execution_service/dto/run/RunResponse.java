package com.flowops.execution_service.dto.run;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class RunResponse {
    private UUID id;
    private UUID flowId;
    private String triggeredBy;
    private String status;
    private Instant startedAt;
    private Instant completedAt;
}
