package com.msa.common.web.response;

import java.util.List;
import java.util.Locale;

public record FieldErrorResponse(String field, Object value, String reason) {

    private static final List<String> SENSITIVE_KEYWORDS = List.of("password", "token", "secret");

    public FieldErrorResponse {
        if (isSensitive(field)) {
            value = null;
        }
    }

    public static FieldErrorResponse of(String field, Object value, String reason) {
        return new FieldErrorResponse(field, value, reason);
    }

    private static boolean isSensitive(String field) {
        if (field == null) {
            return false;
        }
        String lower = field.toLowerCase(Locale.ROOT);
        return SENSITIVE_KEYWORDS.stream().anyMatch(lower::contains);
    }
}
