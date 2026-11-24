package com.msa.common.core.error;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

    HttpStatus status();

    String code();

    String message();
}
