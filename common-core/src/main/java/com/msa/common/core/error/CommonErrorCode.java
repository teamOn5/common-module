package com.msa.common.core.error;

import org.springframework.http.HttpStatus;

public enum CommonErrorCode implements ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON4000", "입력값이 올바르지 않습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "COMMON4001", "입력값의 형식이 올바르지 않습니다."),
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "COMMON4002", "필수 값이 누락되었습니다."),
    INVALID_JSON(HttpStatus.BAD_REQUEST, "COMMON4003", "요청 본문을 읽을 수 없습니다. 형식을 확인해 주세요."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON4010", "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON4030", "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON4040", "요청한 정보를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON4050", "지원하지 않는 요청 방식입니다."),
    CONFLICT(HttpStatus.CONFLICT, "COMMON4090", "요청이 현재 상태와 충돌합니다."),
    ALREADY_DELETED(HttpStatus.CONFLICT, "COMMON4091", "이미 삭제된 정보입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON5000", "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."),
    EXTERNAL_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "COMMON5020", "외부 서비스 연동 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    CommonErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
