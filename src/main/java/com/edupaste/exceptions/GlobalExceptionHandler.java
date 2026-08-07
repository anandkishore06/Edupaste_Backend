package com.edupaste.exceptions;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Let Spring Security handle its own exceptions — do NOT swallow them as 400
    @ExceptionHandler(AccessDeniedException.class)
    public void handleAccessDeniedException(AccessDeniedException ex) throws AccessDeniedException {
        throw ex;
    }

    @ExceptionHandler(AuthenticationException.class)
    public void handleAuthenticationException(AuthenticationException ex) throws AuthenticationException {
        throw ex;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        String causeMsg = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
        String lowerCause = causeMsg != null ? causeMsg.toLowerCase() : "";

        if (lowerCause.contains("duplicate") || lowerCause.contains("unique")) {
            response.put("message", "A record with this name or code already exists.");
        } else if (lowerCause.contains("foreign key") || lowerCause.contains("fk_") || lowerCause.contains("referenced") || lowerCause.contains("violates foreign key")) {
            response.put("message", "Cannot modify or delete this record because it is referenced by other records (e.g. sections or students). Please remove related items first.");
        } else {
            response.put("message", causeMsg != null && !causeMsg.isEmpty() ? causeMsg : "Database constraint violation occurred.");
        }
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatusException(ResponseStatusException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getReason());
        return new ResponseEntity<>(response, ex.getStatusCode());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Invalid request payload: " + ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
        ex.printStackTrace(); // Log the actual exception for debugging
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobalException(Exception ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred.");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
