package com.spring.blog.exception;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<Map<String, Object>> handleBaseException(BaseException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
            "status", ex.getStatus().value(),
            "message", ex.getMessage(),
            "timestamp", LocalDateTime.now()
        ));
    }
}
