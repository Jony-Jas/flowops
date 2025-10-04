package com.flowops.auth_service.mapper;

import org.mapstruct.Mapper;

import com.flowops.auth_service.dto.TokenResponse;
import com.flowops.auth_service.dto.VerifyResponse;
import com.flowops.auth_service.model.KeycloakTokenResponse;
import com.flowops.auth_service.model.TokenPayload;

@Mapper(componentModel = "spring")
public interface TokenMapper {
    VerifyResponse toVerifyResponse(TokenPayload payload);
    TokenResponse toTokenResponse(KeycloakTokenResponse token);
}
