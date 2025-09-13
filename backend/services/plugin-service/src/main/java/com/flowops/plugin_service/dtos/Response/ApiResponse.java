package com.flowops.plugin_service.dtos.Response;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private T data;
    private List<ErrorDetail> errors;
    private ResponseMetaData metadata;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .data(data)
                .metadata(ResponseMetaData.builder()
                        .success(true)
                        .timestamp(Instant.now())
                        .build())
                .build();
    }

    public static <T> ApiResponse<T> success(T data, ResponseMetaData metaData) {
        metaData.setSuccess(true);
        metaData.setTimestamp(Instant.now());
        return ApiResponse.<T>builder()
                .data(data)
                .metadata(metaData)
                .build();
    }

    public static ApiResponse<Void> error(String message, String code) {
        return error(List.of(ErrorDetail.builder()
                .message(message)
                .code(code)
                .build()));
    }

    public static ApiResponse<Void> error(List<ErrorDetail> errors) {
        return ApiResponse.<Void>builder()
                .errors(errors)
                .metadata(ResponseMetaData.builder()
                        .success(false)
                        .timestamp(Instant.now())
                        .build())
                .build();
    }
}
