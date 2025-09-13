package com.flowops.plugin_service.services;

import java.io.InputStream;
import java.util.List;

import com.flowops.plugin_service.domain.entities.PluginMetaData;
import com.flowops.plugin_service.dtos.PluginMetaDataPostDto;

public interface PluginService {
    List<PluginMetaData> getAllPlugins();
    PluginMetaData createPlugin(PluginMetaDataPostDto dto);
    InputStream downloadPlugin(String pluginId, String version);
    List<PluginMetaData> getPluginsByPluginId(String pluginId);
    PluginMetaData getPluginById(String pluginId, String id);
    void deletePluginsByPluginId(String pluginId);
    void deletePluginById(String pluginId, String id);
}