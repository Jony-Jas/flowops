package com.flowops.plugin_service.dtos;

import com.flowops.plugin_service.domain.entities.PluginIOType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PluginIOGetDto {
    private String name;
    private PluginIOType type;
}
