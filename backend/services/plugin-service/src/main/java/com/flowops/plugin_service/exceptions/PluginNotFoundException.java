package com.flowops.plugin_service.exceptions;

import org.springframework.http.HttpStatus;

public class PluginNotFoundException extends BaseException {
    public PluginNotFoundException(String pluginId, String id) {
        super(
            String.format("Plugin not found with pluginId: %s and id: %s", pluginId, id),
            "PLUGIN_NOT_FOUND",
            HttpStatus.NOT_FOUND
        );
    }
}
