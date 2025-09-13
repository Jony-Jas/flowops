package com.flowops.plugin_service.domain.entities;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Document(collection = "plugins")
public class PluginMetaData {

    @Id
    private UUID id;

    private String pluginId;
    private String version;
    private String description;
    private String author;
    private Instant uploadTime;

    private List<PluginIO> inputs;
    private List<PluginIO> outputs;

    private String jarFileUrl; // stored in MinIO
}
