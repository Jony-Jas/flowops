package com.flowops.auth_service.controller;

import com.flowops.auth_service.dto.*;
import com.flowops.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Verify a JWT token’s validity and extract its claims.
     * @param request The VerifyRequest containing the token
     */
    @PostMapping("/verify")
    public ResponseEntity<VerifyResponse> verifyToken(@Valid @RequestBody VerifyRequest request) {
        log.info("Verifying token...");
        VerifyResponse response = authService.verifyToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Request a new token via Keycloak (Client Credentials Flow)
     * @param request TokenRequest containing clientId and secret
     */
    @PostMapping("/token")
    public ResponseEntity<TokenResponse> createToken(@Valid @RequestBody TokenRequest request) {
        log.info("Generating new access token for client: {}", request.getClientId());
        TokenResponse response = authService.createToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh an access token using a valid refresh token
     * @param request RefreshRequest containing the refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refreshToken(@Valid @RequestBody RefreshRequest request) {
        log.info("Refreshing access token...");
        RefreshResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("AuthService is running 🚀");
    }
}
