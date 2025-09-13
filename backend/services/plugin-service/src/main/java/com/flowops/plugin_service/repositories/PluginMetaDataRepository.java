package com.flowops.plugin_service.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.flowops.plugin_service.domain.entities.PluginMetaData;

@Repository
public interface PluginMetaDataRepository extends MongoRepository<PluginMetaData, UUID> {
    boolean existsByPluginIdAndVersion(String pluginId, String version);
    Optional<PluginMetaData> findByPluginIdAndId(String pluginId, UUID id);
    List<PluginMetaData> findAllByPluginId(String pluginId);
    void deleteAllByPluginId(String pluginId);
}
