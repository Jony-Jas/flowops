package com.flowops.plugin_service.dtos;

import com.flowops.plugin_service.domain.entities.PluginIOType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PluginIOPostDto {
    @NotBlank
    private String name;

    @NotNull
    private PluginIOType type;
}
