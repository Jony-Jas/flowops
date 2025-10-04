package com.flowops.auth_service.service;

import com.flowops.auth_service.dto.*;
import com.flowops.auth_service.exception.InvalidTokenException;
import com.flowops.auth_service.mapper.TokenMapper;
import com.flowops.auth_service.model.KeycloakTokenResponse;
import com.flowops.auth_service.model.TokenPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final KeycloakService keycloakService;
    private final JwtVerifier jwtVerifier;
    private final TokenMapper mapper;

    @Override
    public VerifyResponse verifyToken(VerifyRequest request) {
        try {
            TokenPayload payload = jwtVerifier.verifyAndDecode(request.getToken());
            return VerifyResponse.builder()
                .valid(true)
                .subject(payload.getSubject())
                .username((String) payload.getClaims().get("preferred_username"))
                .email((String) payload.getClaims().get("email"))
                .roles(payload.getRoles())
                .expiresAt(payload.getExpiresAt())
                .issuer(payload.getIssuer())
                .message("Token verified successfully")
                .build();

        } catch (Exception e) {
            throw new InvalidTokenException("Invalid or expired JWT token");
        }
    }

    @Override
    public TokenResponse createToken(TokenRequest request) {
        KeycloakTokenResponse response = keycloakService.getAccessToken(
                request.getClientId(),
                request.getClientSecret(),
                request.getScope(),
                request.getUsername(),
                request.getPassword()
        );
        return mapper.toTokenResponse(response);
    }


    @Override
    public RefreshResponse refreshToken(RefreshRequest request) {
        KeycloakTokenResponse response = keycloakService.refreshAccessToken(request.getRefreshToken());
        return RefreshResponse.builder()
                .accessToken(response.getAccessToken())
                .refreshToken(response.getRefreshToken())
                .tokenType(response.getTokenType())
                .expiresIn(response.getExpiresIn())
                .build();
    }
}

