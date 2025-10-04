package com.flowops.auth_service.exception;

import org.springframework.http.HttpStatus;

public class TokenGenerationException extends BaseException {
    public TokenGenerationException(String message) {
        super(message, HttpStatus.BAD_GATEWAY);
    }
}