package com.flowops.plugin_service.exceptions;

import org.springframework.http.HttpStatus;

public class PluginAlreadyExistsException extends BaseException {
    public PluginAlreadyExistsException(String pluginId, String version) {
        super(
            String.format("Plugin with id %s and version %s already exists", pluginId, version),
            "PLUGIN_ALREADY_EXISTS",
            HttpStatus.CONFLICT
        );
    }
}