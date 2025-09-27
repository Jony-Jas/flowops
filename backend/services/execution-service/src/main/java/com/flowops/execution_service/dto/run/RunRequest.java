package com.flowops.execution_service.dto.run;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class RunRequest {
    @NotNull
    private UUID flowId;

    @NotNull
    private String triggeredBy;  // userId | system
}
