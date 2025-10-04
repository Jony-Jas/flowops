package com.flowops.auth_service.service;

import com.flowops.auth_service.exception.KeycloakCommunicationException;
import com.flowops.auth_service.exception.TokenGenerationException;
import com.flowops.auth_service.model.KeycloakTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakServiceImpl implements KeycloakService {

    @Value("${keycloak.token-url}")
    private String tokenUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public KeycloakTokenResponse getAccessToken(String clientId, String clientSecret, String scope, String username, String password) {
        try {
            String grantType = (username != null && !username.isBlank())
                    ? "password"
                    : "client_credentials";

            StringBuilder body = new StringBuilder()
                    .append("client_id=").append(clientId)
                    .append("&client_secret=").append(clientSecret)
                    .append("&grant_type=").append(grantType);

            if (scope != null && !scope.isBlank()) {
                body.append("&scope=").append(scope);
            }

            if (username != null && password != null) {
                body.append("&username=").append(username)
                    .append("&password=").append(password);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);

            ResponseEntity<KeycloakTokenResponse> response =
                    restTemplate.postForEntity(tokenUrl, entity, KeycloakTokenResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new TokenGenerationException("Failed to retrieve access token from Keycloak: " + response.getStatusCode());
            }

        } catch (RestClientException e) {
            log.error("Error communicating with Keycloak", e);
            throw new KeycloakCommunicationException("Unable to connect to Keycloak: " + e.getMessage());
        }
    }


    @Override
    public KeycloakTokenResponse refreshAccessToken(String refreshToken) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("grant_type", "refresh_token");
            params.put("refresh_token", refreshToken);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(params, headers);

            ResponseEntity<KeycloakTokenResponse> response =
                    restTemplate.postForEntity(tokenUrl, entity, KeycloakTokenResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new TokenGenerationException("Failed to refresh access token");
            }
        } catch (RestClientException e) {
            throw new KeycloakCommunicationException("Unable to refresh token via Keycloak");
        }
    }
}
