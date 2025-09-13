package com.flowops.plugin_service.mappers;

import java.util.List;

import org.mapstruct.Mapper;

import com.flowops.plugin_service.domain.entities.PluginIO;
import com.flowops.plugin_service.dtos.PluginIOGetDto;
import com.flowops.plugin_service.dtos.PluginIOPostDto;

@Mapper(componentModel = "spring")
public interface PluginIOMapper {

    PluginIOGetDto toGetDto(PluginIO io);
    List<PluginIOGetDto> toGetDtoList(List<PluginIO> ios);

    PluginIO toEntity(PluginIOPostDto dto);
    List<PluginIO> toEntityList(List<PluginIOPostDto> dtos);
}