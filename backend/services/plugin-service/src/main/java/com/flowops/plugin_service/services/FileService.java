package com.flowops.plugin_service.services;

import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    String uploadFile(MultipartFile file, String pluginId, String version);
    InputStream downloadFile(String pluginId, String version);
    void deleteFile(String pluginId, String version);
}
