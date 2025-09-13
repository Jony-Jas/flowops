package com.flowops.plugin_service.domain.entities;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PluginIO {
    private String name;
    private PluginIOType type;
}
