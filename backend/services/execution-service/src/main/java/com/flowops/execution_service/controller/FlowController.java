package com.flowops.execution_service.controller;

import com.flowops.execution_service.dto.flow.FlowDetailResponse;
import com.flowops.execution_service.dto.flow.FlowRequest;
import com.flowops.execution_service.dto.flow.FlowResponse;
import com.flowops.execution_service.service.ExecutionService;
import com.flowops.shared_api.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/executions/flows")
@RequiredArgsConstructor
public class FlowController {

    private final ExecutionService executionService;

    @PostMapping
    public ResponseEntity<ApiResponse<FlowResponse>> createFlow(
            @Valid @RequestBody FlowRequest request,
            @RequestHeader("X-User-Id") UUID ownerId
    ) {
        FlowResponse response = executionService.createFlow(request, ownerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FlowResponse>>> getAllFlows(
            @RequestHeader("X-User-Id") UUID ownerId
    ) {
        List<FlowResponse> flows = executionService.getAllFlows(ownerId);
        return ResponseEntity.ok(ApiResponse.success(flows));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FlowDetailResponse>> getFlow(
            @PathVariable UUID id
    ) {
        FlowDetailResponse flow = executionService.getFlow(id);
        return ResponseEntity.ok(ApiResponse.success(flow));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FlowResponse>> updateFlow(
            @PathVariable UUID id,
            @Valid @RequestBody FlowRequest request,
            @RequestHeader("X-User-Id") UUID ownerId
    ) {
        FlowResponse response = executionService.updateFlow(id, request, ownerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFlow(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID ownerId
    ) {
        executionService.deleteFlow(id, ownerId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
