package com.example.brightpath.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, Object>> handle(ApiException exception) {
        return ResponseEntity.status(exception.getStatus()).body(Map.of(
                "status", exception.getStatus().value(),
                "message", exception.getMessage(),
                "timestamp", Instant.now().toString()));
    }
}
