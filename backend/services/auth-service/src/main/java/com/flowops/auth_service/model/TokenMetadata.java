package com.flowops.auth_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenMetadata {
    private boolean valid;
    private String algorithm;
    private String keyId;
    private Instant verifiedAt;
    private String message; // Optional info like “signature valid”, “expired”, etc.
}
