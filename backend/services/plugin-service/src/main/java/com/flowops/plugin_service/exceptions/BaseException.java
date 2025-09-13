package com.flowops.plugin_service.exceptions;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;
    private final Object[] args; // Arguments for dynamic message formatting
    
    protected BaseException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.args = null;
    }
    
    protected BaseException(String message, String errorCode, HttpStatus httpStatus, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.args = args;
    }
    
    protected BaseException(String message, Throwable cause, String errorCode, HttpStatus httpStatus) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.args = null;
    }
}
