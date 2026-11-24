package com.msa.common.web.response;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.msa.common.core.error.ErrorCode;

public final class ApiResponses {

    private ApiResponses() {
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return toEntity(ApiResponse.ok(data));
    }

    public static ResponseEntity<ApiResponse<Void>> ok() {
        return toEntity(ApiResponse.ok());
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return toEntity(ApiResponse.created(data));
    }

    public static ResponseEntity<ApiResponse<Void>> fail(ErrorCode errorCode) {
        return toEntity(ApiResponse.fail(errorCode));
    }

    public static ResponseEntity<ApiResponse<Void>> fail(ErrorCode errorCode, List<FieldErrorResponse> errors) {
        return toEntity(ApiResponse.fail(errorCode, errors));
    }

    private static <T> ResponseEntity<ApiResponse<T>> toEntity(ApiResponse<T> apiResponse) {
        return ResponseEntity.status(apiResponse.status()).body(apiResponse);
    }
}
