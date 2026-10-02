package com.leetsync.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<?> handleApi(
            ApiException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", ex.getStatus().value(),
                "code", ex.getCode(),
                "message", ex.getMessage(),
                "path", request.getRequestURI()
        ));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<?> handleMissingHeader(
            MissingRequestHeaderException ex,
            HttpServletRequest request
    ) {
        String header = ex.getHeaderName();

        if ("Authorization".equalsIgnoreCase(header)) {
            return ResponseEntity.status(401).body(Map.of(
                    "timestamp", Instant.now().toString(),
                    "status", 401,
                    "code", "MISSING_TOKEN",
                    "message", "Authorization header is required.",
                    "path", request.getRequestURI()
            ));
        }

        return ResponseEntity.badRequest().body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", 400,
                "code", "MISSING_HEADER",
                "message", "Required header is missing: " + header,
                "path", request.getRequestURI()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> handleValidation(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = new java.util.HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(error.getField(), error.getDefaultMessage())
                );

        return ResponseEntity.badRequest().body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", 400,
                "code", "VALIDATION_ERROR",
                "errors", errors
        ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<?> handleConstraint(
            ConstraintViolationException ex
    ) {
        return ResponseEntity.badRequest().body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", 400,
                "code", "VALIDATION_ERROR",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<?> handleUnexpected(Exception ex) {
        return ResponseEntity.internalServerError().body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", 500,
                "code", "INTERNAL_ERROR",
                "message", "An unexpected server error occurred."
        ));
    }
}