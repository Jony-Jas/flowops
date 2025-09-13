package com.flowops.plugin_service.services;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.flowops.plugin_service.domain.entities.PluginMetaData;
import com.flowops.plugin_service.dtos.PluginMetaDataPostDto;
import com.flowops.plugin_service.exceptions.InvalidFileException;
import com.flowops.plugin_service.exceptions.PluginAlreadyExistsException;
import com.flowops.plugin_service.exceptions.PluginNotFoundException;
import com.flowops.plugin_service.mappers.PluginMetaDataMapper;
import com.flowops.plugin_service.repositories.PluginMetaDataRepository;

@Service
public class PluginServiceImpl implements PluginService {

    @Autowired
    private PluginMetaDataRepository pluginMetaDataRepository;

    @Autowired
    private FileService fileService;

    @Autowired
    private PluginMetaDataMapper pluginMetaDataMapper;

    @Override
    public List<PluginMetaData> getAllPlugins() {
        return pluginMetaDataRepository.findAll();
    }

    @Override
    public PluginMetaData createPlugin(PluginMetaDataPostDto dto) {
        validatePluginCreation(dto);

        PluginMetaData entity = pluginMetaDataMapper.toEntity(dto);
        MultipartFile file = dto.getJarFile();

        fileService.uploadFile(file, dto.getPluginId(), dto.getVersion());

        entity.setUploadTime(Instant.now());
        entity.setId(UUID.randomUUID());
        entity.setJarFileUrl("/api/plugins/" + entity.getPluginId() + "/" + entity.getId());

        return pluginMetaDataRepository.save(entity);
    }

    private void validatePluginCreation(PluginMetaDataPostDto dto) {
        if (pluginMetaDataRepository.existsByPluginIdAndVersion(dto.getPluginId(), dto.getVersion())) {
            throw new PluginAlreadyExistsException(dto.getPluginId(), dto.getVersion());
        }

        MultipartFile file = dto.getJarFile();
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File must be provided and cannot be empty.");
        }

        if (!file.getOriginalFilename().endsWith(".jar")) {
            throw new InvalidFileException("Only .jar files are allowed.");
        }

        if (file.getSize() > 50 * 1024 * 1024) {
            throw new InvalidFileException("File size exceeds the maximum limit of 50 MB.");
        }
    }

    @Override
    public InputStream downloadPlugin(String pluginId, String id) {
        PluginMetaData pluginMetaData = pluginMetaDataRepository
            .findByPluginIdAndId(pluginId, UUID.fromString(id))
            .orElseThrow(() -> new PluginNotFoundException(pluginId, id));

        return fileService.downloadFile(pluginMetaData.getPluginId(), pluginMetaData.getVersion());
    }

    @Override
    public List<PluginMetaData> getPluginsByPluginId(String pluginId) {
        List<PluginMetaData> plugins = pluginMetaDataRepository.findAllByPluginId(pluginId);
        
        return plugins;
    }


    @Override
    public PluginMetaData getPluginById(String pluginId, String id) {
        return pluginMetaDataRepository
            .findByPluginIdAndId(pluginId, UUID.fromString(id))
            .orElseThrow(() -> new PluginNotFoundException(pluginId, id));
    }

    @Override
    public void deletePluginsByPluginId(String pluginId) {
        List<PluginMetaData> plugins = pluginMetaDataRepository.findAllByPluginId(pluginId);

        for (PluginMetaData plugin : plugins) {
            fileService.deleteFile(plugin.getPluginId(), plugin.getVersion());
        }
        
        pluginMetaDataRepository.deleteAllByPluginId(pluginId);
    }

    @Override
    public void deletePluginById(String pluginId, String id) {
        PluginMetaData plugin = pluginMetaDataRepository
            .findByPluginIdAndId(pluginId, UUID.fromString(id))
            .orElseThrow(() -> new PluginNotFoundException(pluginId, id));

        fileService.deleteFile(plugin.getPluginId(), plugin.getVersion());
        pluginMetaDataRepository.delete(plugin);
    }
}
