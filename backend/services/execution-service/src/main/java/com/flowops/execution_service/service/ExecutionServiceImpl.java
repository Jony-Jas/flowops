package com.flowops.execution_service.service;

import com.flowops.execution_service.dto.flow.FlowDetailResponse;
import com.flowops.execution_service.dto.flow.FlowRequest;
import com.flowops.execution_service.dto.flow.FlowResponse;
import com.flowops.execution_service.exception.NotFoundException;
import com.flowops.execution_service.mapper.FlowMapper;
import com.flowops.execution_service.mapper.FlowUpdateRequest;
import com.flowops.execution_service.model.Flow;
import com.flowops.execution_service.repository.FlowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

// TODO: Implement Scheduling
@Service
@RequiredArgsConstructor
public class ExecutionServiceImpl implements ExecutionService {

    private final FlowRepository flowRepository;
    private final FlowMapper flowMapper;

    @Override
    public FlowResponse createFlow(FlowRequest request, UUID ownerId) {
        Flow flow = Flow.builder()
                .id(UUID.randomUUID())
                .name(request.getName())
                .description(request.getDescription())
                .ownerId(ownerId)
                .build();
        //TODO: Created data is null
        return flowMapper.toResponse(flowRepository.save(flow));
    }

    @Override
    public List<FlowResponse> getAllFlows(UUID ownerId) {
        return flowRepository.findAll().stream()
                .filter(flow -> flow.getOwnerId().equals(ownerId))
                .map(flowMapper::toResponse)
                .toList();
    }

    @Override
    public FlowDetailResponse getFlow(UUID id) {
        Flow flow = flowRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Flow not found: " + id));
        return flowMapper.toDetailResponse(flow);
    }

    @Override
    public FlowResponse updateFlow(UUID id, FlowUpdateRequest request, UUID ownerId) {
        Flow flow = flowRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Flow not found: " + id));

        if (!flow.getOwnerId().equals(ownerId)) {
            throw new NotFoundException("Flow not found for owner: " + ownerId);
        }

        flow.setName(request.getName());
        flow.setDescription(request.getDescription());
        if (request.getCron() != null) {
            flow.setSchedule(Flow.Schedule.builder()
                    .type(Flow.ScheduleType.CRON)
                    .cron(request.getCron())
                    .build());
        }
        if (request.getSteps() != null) {
            // Map DTO → Entity
            List<Flow.Step> steps = request.getSteps().stream().map(this::mapStep).toList();
            flow.setSteps(steps);
        }

        return flowMapper.toResponse(flowRepository.save(flow));
    }

    @Override
    public void deleteFlow(UUID id, UUID ownerId) {
        Flow flow = flowRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Flow not found: " + id));
        if (!flow.getOwnerId().equals(ownerId)) {
            throw new NotFoundException("Flow not found for owner: " + ownerId);
        }
        flowRepository.delete(flow);
    }

    private Flow.Step mapStep(FlowUpdateRequest.StepDto dto) {
        return Flow.Step.builder()
                .stepId(dto.getStepId() != null ? dto.getStepId() : UUID.randomUUID())
                .pluginId(dto.getPluginId())
                .type(Flow.StepType.valueOf(dto.getType().toUpperCase()))
                .inputs(dto.getInputs())
                .outputs(dto.getOutputs())
                .condition(dto.getCondition())
                .branches(dto.getBranches() != null
                        ? dto.getBranches().stream().map(this::mapBranch).toList()
                        : null)
                .children(dto.getChildren() != null
                        ? dto.getChildren().stream().map(this::mapStep).toList()
                        : null)
                .config(dto.getConfig() != null ? Flow.Config.builder()
                        .timeoutMs(dto.getConfig().getTimeoutMs())
                        .retryCount(dto.getConfig().getRetryCount())
                        .retryDelayMs(dto.getConfig().getRetryDelayMs())
                        .build() : null)
                .build();
    }

    private Flow.Branch mapBranch(FlowUpdateRequest.BranchDto dto) {
        return Flow.Branch.builder()
                .branchId(dto.getBranchId() != null ? dto.getBranchId() : UUID.randomUUID())
                .condition(dto.getCondition())
                .steps(dto.getSteps() != null ? dto.getSteps().stream().map(this::mapStep).toList() : null)
                .build();
    }
}
