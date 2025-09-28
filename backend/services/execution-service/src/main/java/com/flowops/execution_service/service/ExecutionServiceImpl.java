package com.flowops.execution_service.service;

import com.flowops.execution_service.dto.flow.FlowDetailResponse;
import com.flowops.execution_service.dto.flow.FlowRequest;
import com.flowops.execution_service.dto.flow.FlowResponse;
import com.flowops.execution_service.exception.NotFoundException;
import com.flowops.execution_service.mapper.FlowMapper;
import com.flowops.execution_service.model.Flow;
import com.flowops.execution_service.repository.FlowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

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
    public FlowResponse updateFlow(UUID id, FlowRequest request, UUID ownerId) {
        Flow flow = flowRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Flow not found: " + id));
        if (!flow.getOwnerId().equals(ownerId)) {
            throw new NotFoundException("Flow not found for owner: " + ownerId);
        }
        flow.setName(request.getName());
        flow.setDescription(request.getDescription());
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
}
