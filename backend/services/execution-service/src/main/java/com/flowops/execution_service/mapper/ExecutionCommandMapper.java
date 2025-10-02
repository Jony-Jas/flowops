package com.flowops.execution_service.mapper;

import com.flowops.execution_service.model.Flow;
import com.flowops.kafka_contracts.events.ExecutionCommandEvent;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ExecutionCommandMapper {

    // Map Flow.Step → ExecutionCommandEvent.Step
    ExecutionCommandEvent.Step toCommandStep(Flow.Step step);

    // Map Flow.Branch → ExecutionCommandEvent.Branch
    ExecutionCommandEvent.Branch toCommandBranch(Flow.Branch branch);

    // Map Flow.Config → ExecutionCommandEvent.Config
    ExecutionCommandEvent.Config toCommandConfig(Flow.Config config);
}