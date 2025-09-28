package com.flowops.execution_service.service;

import com.flowops.execution_service.dto.run.RunDetailResponse;
import com.flowops.execution_service.dto.run.RunRequest;

import java.util.UUID;

public interface RunService {
    UUID startRun(UUID flowId, RunRequest request);
    void pauseRun(UUID runId);
    void resumeRun(UUID runId);
    void stopRun(UUID runId);
    RunDetailResponse getRunStatus(UUID runId);
}
