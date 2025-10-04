package com.flowops.auth_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenPayload {
    private String subject;              // sub
    private String issuer;               // iss
    private Instant issuedAt;            // iat
    private Instant expiresAt;           // exp
    private List<String> roles;          // realm_access.roles or resource_access
    private Map<String, Object> claims;  // All claims for convenience
}
