package com.dreamgames.backendengineeringcasestudy.exception;

import org.springframework.http.HttpStatus;

public class CustomAppException extends RuntimeException {
    private final HttpStatus status;

    public CustomAppException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
