package com.flowops.auth_service.service;

import com.flowops.auth_service.model.TokenPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JwtVerifier {

    private final JwtDecoder jwtDecoder;

    public JwtVerifier(@Value("${keycloak.jwks-url}") String jwksUrl) {
        this.jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwksUrl).build();
    }

    public TokenPayload verifyAndDecode(String token) {
        Jwt jwt = jwtDecoder.decode(token);

        Map<String, Object> realmAccess = (Map<String, Object>) jwt.getClaims().get("realm_access");
        List<String> roles = realmAccess != null ? (List<String>) realmAccess.get("roles") : List.of();

        String preferredUsername = (String) jwt.getClaims().getOrDefault("preferred_username", null);
        String email = (String) jwt.getClaims().getOrDefault("email", null);

        return TokenPayload.builder()
                .subject(jwt.getSubject())
                .issuer(jwt.getIssuer().toString())
                .issuedAt(jwt.getIssuedAt())
                .expiresAt(jwt.getExpiresAt())
                .roles(roles)
                .claims(jwt.getClaims())
                .build();
    }
}
