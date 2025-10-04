package com.flowops.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyResponse {
    private boolean valid;
    private String subject;
    private String username;   // <— NEW
    private String email;      // <— NEW
    private List<String> roles;
    private Instant expiresAt;
    private String issuer;
    private String message;
}

