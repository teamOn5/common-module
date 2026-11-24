package com.msa.common.core.error;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;

public final class ErrorCodeValidator {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z]+\\d{4}$");
    private static final int MIN_STATUS = 400;
    private static final int MAX_STATUS = 599;

    private ErrorCodeValidator() {
    }

    public static void validate(ErrorCode[] codes) {
        Objects.requireNonNull(codes, "codes 는 null 일 수 없습니다.");
        Set<String> seen = new HashSet<>();
        for (ErrorCode errorCode : codes) {
            String name = describe(errorCode);
            String code = errorCode.code();
            HttpStatus status = errorCode.status();

            if (code == null || !CODE_PATTERN.matcher(code).matches()) {
                throw new IllegalStateException(
                        name + ": code '" + code + "' 가 형식 " + CODE_PATTERN.pattern() + " 에 맞지 않습니다.");
            }
            if (status == null) {
                throw new IllegalStateException(name + ": status 가 null 입니다.");
            }
            if (status.value() < MIN_STATUS || status.value() > MAX_STATUS) {
                throw new IllegalStateException(name + ": status " + status.value() + " 가 "
                        + MIN_STATUS + "~" + MAX_STATUS + " 범위를 벗어났습니다.");
            }
            int statusInCode = Integer.parseInt(code.substring(code.length() - 4, code.length() - 1));
            if (statusInCode != status.value()) {
                throw new IllegalStateException(name + ": code '" + code + "' 의 상태값 " + statusInCode
                        + " 이 status " + status.value() + " 와 다릅니다.");
            }
            if (!seen.add(code)) {
                throw new IllegalStateException(name + ": code '" + code + "' 가 중복되었습니다.");
            }
        }
    }

    private static String describe(ErrorCode errorCode) {
        if (errorCode instanceof Enum<?> e) {
            return e.getDeclaringClass().getSimpleName() + "." + e.name();
        }
        return String.valueOf(errorCode);
    }
}
