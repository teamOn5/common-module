package com.msa.common.core.error;

import java.util.Objects;

public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, null, null);
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        this(errorCode, detail, null);
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        this(errorCode, null, cause);
    }

    private BusinessException(ErrorCode errorCode, String detail, Throwable cause) {
        super(resolveMessage(errorCode, detail), cause);
        this.errorCode = errorCode;
    }

    private static String resolveMessage(ErrorCode errorCode, String detail) {
        Objects.requireNonNull(errorCode, "errorCode 는 null 일 수 없습니다.");
        if (detail == null || detail.isBlank()) {
            return errorCode.message();
        }
        return detail;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * detail 이 있으면 detail(로그용), 없으면 {@code errorCode.message()}.
     * 사용자 응답에는 이 값이 아니라 {@code getErrorCode().message()} 를 사용한다.
     */
    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
