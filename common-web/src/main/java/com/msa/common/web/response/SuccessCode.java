package com.msa.common.web.response;

import org.springframework.http.HttpStatus;

public enum SuccessCode {

    OK(HttpStatus.OK, "COMMON2000", "요청이 성공했습니다."),
    CREATED(HttpStatus.CREATED, "COMMON2010", "생성되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    SuccessCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
