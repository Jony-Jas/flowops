package com.flowops.execution_service.dto.flow;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FlowRequest {
    @NotBlank
    private String name;

    private String description;

    // For now we won’t allow steps to be created directly here;
    // steps can be added via a separate update API if needed.
    private String cron;   // Optional scheduling
}
