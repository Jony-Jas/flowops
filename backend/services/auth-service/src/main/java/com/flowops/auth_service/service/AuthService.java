package com.flowops.auth_service.service;

import com.flowops.auth_service.dto.*;

public interface AuthService {
    VerifyResponse verifyToken(VerifyRequest request);
    TokenResponse createToken(TokenRequest request);
    RefreshResponse refreshToken(RefreshRequest request);
}
