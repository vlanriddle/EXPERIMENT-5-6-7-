package com.example.social.common.api;

import java.time.Instant;
import java.util.List;

public record ApiResponse<T>(
        Instant timestamp,
        int status,
        String message,
        T data,
        List<String> errors) {

    public static <T> ApiResponse<T> success(int status, String message, T data) {
        return new ApiResponse<>(Instant.now(), status, message, data, List.of());
    }

    public static ApiResponse<Void> failure(int status, String message, List<String> errors) {
        return new ApiResponse<>(Instant.now(), status, message, null, errors);
    }
}
