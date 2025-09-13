package com.flowops.plugin_service.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
public class PluginMetaDataPostDto {
    @NotBlank
    private String pluginId;
    
    @NotBlank
    private String version;
    
    @NotBlank
    private String description;
    
    @NotBlank
    private String author;
    
    @NotEmpty
    private List<PluginIOPostDto> inputs;
    
    @NotEmpty
    private List<PluginIOPostDto> outputs;

    @NotNull
    private MultipartFile jarFile;
}