package com.flowops.execution_engine.mapper;

import com.flowops.execution_engine.model.Step;
import com.flowops.kafka_contracts.events.ExecutionCommandEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * MapStruct mapper to convert ExecutionCommandEvent.Step -> engine Step.
 * - Recursively maps children and branches
 * - Converts config into Map<String,Object> using ConfigConverters.protoToMap
 *
 * Make sure MapStruct annotation processor is enabled in your build.
 */
@Mapper(componentModel = "spring", uses = { ConfigConverters.class })
public interface ExecutionEventMapper {

    ExecutionEventMapper INSTANCE = Mappers.getMapper(ExecutionEventMapper.class);

    /**
     * Map a single Step from the ExecutionCommandEvent to engine Step.
     * - maps stepId, pluginId, inputs, outputs, condition, children, branches
     * - converts config using ConfigConverters.protoToMap (qualified by name "protoToMap")
     */
    @Mapping(target = "config", source = "config", qualifiedByName = "protoToMap")
    Step toDomain(ExecutionCommandEvent.Step step);

    /**
     * Map list of proto steps to list of engine steps (recursive mapping of children/branches).
     */
    List<Step> toDomainList(List<ExecutionCommandEvent.Step> steps);
}