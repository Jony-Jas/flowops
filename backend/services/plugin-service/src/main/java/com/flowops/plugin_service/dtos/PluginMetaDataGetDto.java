package com.flowops.plugin_service.dtos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PluginMetaDataGetDto {
    private UUID id;

    private String pluginId;
    private String version;
    private String description;
    private String author;
    private Instant uploadTime;

    private List<PluginIOGetDto> inputs;
    private List<PluginIOGetDto> outputs;

    private String jarFileUrl;
}