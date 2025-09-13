package com.flowops.plugin_service.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidFileException extends BaseException {
    public InvalidFileException(String message) {
        super(message, "INVALID_FILE", HttpStatus.BAD_REQUEST);
    }
}