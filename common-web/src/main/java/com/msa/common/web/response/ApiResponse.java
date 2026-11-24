package com.msa.common.web.response;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.msa.common.core.error.ErrorCode;

@JsonPropertyOrder({"status", "code", "message", "data", "errors", "timestamp"})
public record ApiResponse<T>(
        int status,
        String code,
        String message,
        T data,
        List<FieldErrorResponse> errors,
        Instant timestamp
) {

    public ApiResponse {
        errors = errors == null || errors.isEmpty() ? null : List.copyOf(errors);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return success(SuccessCode.OK, data);
    }

    public static <T> ApiResponse<T> ok() {
        return success(SuccessCode.OK, null);
    }

    public static <T> ApiResponse<T> created(T data) {
        return success(SuccessCode.CREATED, data);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return fail(errorCode, List.of());
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, List<FieldErrorResponse> errors) {
        return new ApiResponse<>(errorCode.status().value(), errorCode.code(), errorCode.message(), null, errors, now());
    }

    private static <T> ApiResponse<T> success(SuccessCode successCode, T data) {
        return new ApiResponse<>(successCode.status().value(), successCode.code(), successCode.message(), data,
                List.of(), now());
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
