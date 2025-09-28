package com.flowops.execution_service.mapper;

import com.flowops.execution_service.dto.run.RunDetailResponse;
import com.flowops.execution_service.dto.run.RunResponse;
import com.flowops.execution_service.model.Run;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RunMapper {
    RunResponse toResponse(Run run);
    RunDetailResponse toDetailResponse(Run run);
}
