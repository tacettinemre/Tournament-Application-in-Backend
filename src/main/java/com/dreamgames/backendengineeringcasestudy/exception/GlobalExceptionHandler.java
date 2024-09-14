package com.dreamgames.backendengineeringcasestudy.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomAppException.class)
    public ResponseEntity<Map<String, Object>> handleCustomAppException(CustomAppException ex) {
        // Create a map to store the response data
        Map<String, Object> response = new HashMap<>();
        response.put("message", ex.getMessage());
        response.put("status", ex.getStatus().value());

        // Return the status and the map as the response
        return new ResponseEntity<>(response, ex.getStatus());
    }
}