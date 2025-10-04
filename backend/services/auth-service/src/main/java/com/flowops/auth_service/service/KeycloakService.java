package com.flowops.auth_service.service;

import com.flowops.auth_service.model.KeycloakTokenResponse;

public interface KeycloakService {
    KeycloakTokenResponse getAccessToken(String clientId, String clientSecret, String scope, String username, String password);
    KeycloakTokenResponse refreshAccessToken(String refreshToken);
}
