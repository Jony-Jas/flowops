package com.flowops.auth_service.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class KeycloakConfig {

    @Value("${keycloak.base-url}")
    private String baseUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Value("${keycloak.token-url}")
    private String tokenUrl;

    @Value("${keycloak.jwks-url}")
    private String jwksUrl;

    public String getRealmUrl() {
        return String.format("%s/realms/%s", baseUrl, realm);
    }

    public String getOpenIdConfigUrl() {
        return getRealmUrl() + "/.well-known/openid-configuration";
    }
}