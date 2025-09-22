package com.flowops.shared_api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorDetail {
    private String code;
    private String message;
    private String field; // Optional: field related to the error
    private String rejectedValue; // Optional: the value that was rejected
}
