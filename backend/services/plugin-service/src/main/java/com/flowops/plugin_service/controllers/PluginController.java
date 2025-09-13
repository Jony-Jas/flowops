package com.flowops.plugin_service.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flowops.plugin_service.dtos.PluginMetaDataGetDto;
import com.flowops.plugin_service.dtos.PluginMetaDataPostDto;
import com.flowops.plugin_service.dtos.Response.ApiResponse;
import com.flowops.plugin_service.mappers.PluginMetaDataMapper;
import com.flowops.plugin_service.services.PluginService;

import jakarta.validation.Valid;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/api/plugins")
public class PluginController {

    @Autowired
    private PluginService pluginService;

    @Autowired
    private PluginMetaDataMapper pluginMetaDataMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PluginMetaDataGetDto>>> getAllPlugins() {
        var plugins = pluginMetaDataMapper.toGetDtoList(pluginService.getAllPlugins());
        return ResponseEntity.ok(ApiResponse.success(plugins));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PluginMetaDataGetDto>> createPlugin(@Valid @ModelAttribute PluginMetaDataPostDto dto){
        var pluginMetaData = pluginService.createPlugin(dto);
        var response = ApiResponse.success(pluginMetaDataMapper.toGetDto(pluginMetaData));
        return ResponseEntity.created(URI.create(pluginMetaData.getJarFileUrl())).body(response);
    }

    @GetMapping("/{pluginId}")
    public ResponseEntity<ApiResponse<List<PluginMetaDataGetDto>>> getPluginsByPluginId(@PathVariable String pluginId){
        var plugins = pluginMetaDataMapper.toGetDtoList(pluginService.getPluginsByPluginId(pluginId));
        return ResponseEntity.ok(ApiResponse.success(plugins));
    }

    @GetMapping("/{pluginId}/{id}")
    public ResponseEntity<ApiResponse<PluginMetaDataGetDto>> getPluginById(
        @PathVariable String pluginId,
        @PathVariable String id
    ){
        var plugin = pluginMetaDataMapper.toGetDto(pluginService.getPluginById(pluginId, id));
        return ResponseEntity.ok(ApiResponse.success(plugin));
    }

    @GetMapping("/{pluginId}/{id}/jar")
    public ResponseEntity<Resource> downloadPluginJar(@PathVariable String pluginId, @PathVariable String id) {
        var inputStream = pluginService.downloadPlugin(pluginId, id);
        var resource = new InputStreamResource(inputStream);
        var fileName = pluginId + "-" + id + ".jar";

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
            .header(HttpHeaders.CONTENT_TYPE, "application/java-archive")
            .body(resource);
    }

    @DeleteMapping("/{pluginId}")
    public ResponseEntity<Void> deletePluginsByPluginId(@PathVariable String pluginId){
        pluginService.deletePluginsByPluginId(pluginId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{pluginId}/{id}")
    public ResponseEntity<Void> deletePluginById(@PathVariable String pluginId, @PathVariable String id){
        pluginService.deletePluginById(pluginId, id);
        return ResponseEntity.noContent().build();
    }
}
