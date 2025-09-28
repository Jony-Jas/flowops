package com.flowops.execution_service.mapper;

import com.flowops.execution_service.dto.flow.FlowDetailResponse;
import com.flowops.execution_service.dto.flow.FlowResponse;
import com.flowops.execution_service.model.Flow;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FlowMapper {
    FlowResponse toResponse(Flow flow);
    FlowDetailResponse toDetailResponse(Flow flow);
}
