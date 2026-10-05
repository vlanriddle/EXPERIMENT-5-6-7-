package com.example.social.common.error;

import com.example.social.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiResponse<Void>> notFound(ResourceNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), List.of(exception.getMessage()));
    }

    @ExceptionHandler({UnauthorizedException.class, org.springframework.security.authentication.BadCredentialsException.class})
    ResponseEntity<ApiResponse<Void>> unauthorized(RuntimeException exception) {
        return response(HttpStatus.UNAUTHORIZED, "Authentication failed", List.of("Authentication failed"));
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiResponse<Void>> conflict(ConflictException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), List.of(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> invalidRequest(MethodArgumentNotValidException exception) {
        List<String> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return response(HttpStatus.BAD_REQUEST, "Request validation failed", errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiResponse<Void>> constraintViolation(ConstraintViolationException exception) {
        List<String> errors = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();
        return response(HttpStatus.BAD_REQUEST, "Request validation failed", errors);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse<Void>> integrityConflict(DataIntegrityViolationException exception) {
        return response(HttpStatus.CONFLICT, "The request conflicts with existing data", List.of("The request conflicts with existing data"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiResponse<Void>> invalidArgument(IllegalArgumentException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage(), List.of(exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> unexpected(Exception exception) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", List.of("An unexpected error occurred"));
    }

    private ResponseEntity<ApiResponse<Void>> response(HttpStatus status, String message, List<String> errors) {
        return ResponseEntity.status(status).body(ApiResponse.failure(status.value(), message, errors));
    }
}
