package com.dreamgames.backendengineeringcasestudy.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomAppException.class)
    public ResponseEntity<String> handleCustomAppException(CustomAppException ex) {
        // Return the status and the message from the CustomAppException
        return new ResponseEntity<>(ex.getMessage(), ex.getStatus());
    }
}