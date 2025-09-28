package com.flowops.execution_service.service;

import com.flowops.execution_service.dto.flow.FlowDetailResponse;
import com.flowops.execution_service.dto.flow.FlowRequest;
import com.flowops.execution_service.dto.flow.FlowResponse;

import java.util.List;
import java.util.UUID;

public interface ExecutionService {
    FlowResponse createFlow(FlowRequest request, UUID ownerId);
    List<FlowResponse> getAllFlows(UUID ownerId);
    FlowDetailResponse getFlow(UUID id);
    FlowResponse updateFlow(UUID id, FlowRequest request, UUID ownerId);
    void deleteFlow(UUID id, UUID ownerId);
}
