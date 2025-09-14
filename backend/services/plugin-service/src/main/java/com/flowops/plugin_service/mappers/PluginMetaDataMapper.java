package com.flowops.plugin_service.mappers;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.flowops.plugin_service.domain.entities.PluginMetaData;
import com.flowops.plugin_service.dtos.PluginMetaDataGetDto;
import com.flowops.plugin_service.dtos.PluginMetaDataPostDto;

@Mapper(componentModel = "spring")
public interface PluginMetaDataMapper {

    /**
     * Convert GetEntity to DTO
     */
    PluginMetaDataGetDto toGetDto(PluginMetaData entity);

    /**
     * Convert list of get entities to list of DTOs
     */
    List<PluginMetaDataGetDto> toGetDtoList(List<PluginMetaData> entities);

    /**
     * Convert Post DTO to Entity
     */
    @Mapping(target = "uploadTime", ignore = true)
    @Mapping(target = "jarFileUrl", ignore = true)
    @Mapping(target = "id", ignore = true)
    PluginMetaData toEntity(PluginMetaDataPostDto dto);
}
