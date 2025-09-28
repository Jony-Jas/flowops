package com.flowops.execution_service.controller;

import com.flowops.execution_service.dto.run.RunDetailResponse;
import com.flowops.execution_service.dto.run.RunRequest;
import com.flowops.execution_service.service.RunService;
import com.flowops.shared_api.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/executions")
@RequiredArgsConstructor
public class RunController {

    private final RunService runService;

    @PostMapping("/flows/{flowId}/start")
    public ResponseEntity<ApiResponse<UUID>> startRun(
            @PathVariable UUID flowId,
            @Valid @RequestBody RunRequest request
    ) {
        UUID runId = runService.startRun(flowId, request);
        return ResponseEntity.ok(ApiResponse.success(runId));
    }

    @PostMapping("/runs/{runId}/pause")
    public ResponseEntity<ApiResponse<Void>> pauseRun(@PathVariable UUID runId) {
        runService.pauseRun(runId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/runs/{runId}/resume")
    public ResponseEntity<ApiResponse<Void>> resumeRun(@PathVariable UUID runId) {
        runService.resumeRun(runId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/runs/{runId}/stop")
    public ResponseEntity<ApiResponse<Void>> stopRun(@PathVariable UUID runId) {
        runService.stopRun(runId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/runs/{runId}/status")
    public ResponseEntity<ApiResponse<RunDetailResponse>> getRunStatus(@PathVariable UUID runId) {
        RunDetailResponse status = runService.getRunStatus(runId);
        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
