package com.flowops.plugin_service.dtos.Response;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseMetaData {
    private Instant timestamp;
    private boolean success;
    private String requestId; // For tracking requests
    private Integer totalCount; // For paginated responses
    private Integer pageSize; // For paginated responses  
    private Integer currentPage; // For paginated responses
    private Map<String, Object> additional; // For any additional metadata
}
