package com.nnipa.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Standard API response wrapper
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String error;
    private String correlationId;
    private Instant timestamp;
    private Integer status;

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .timestamp(Instant.now())
                .status(200)
                .build();
    }

    public static <T> ApiResponse<T> error(String error, String message, Integer status) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(error)
                .message(message)
                .timestamp(Instant.now())
                .status(status)
                .build();
    }
}