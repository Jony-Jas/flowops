package com.flowops.auth_service.exception;

import org.springframework.http.HttpStatus;

public class KeycloakCommunicationException extends BaseException {
    public KeycloakCommunicationException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
