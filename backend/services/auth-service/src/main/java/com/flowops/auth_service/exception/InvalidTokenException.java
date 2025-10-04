package com.flowops.auth_service.exception;

import org.springframework.http.HttpStatus;

public class InvalidTokenException extends BaseException {
    public InvalidTokenException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
