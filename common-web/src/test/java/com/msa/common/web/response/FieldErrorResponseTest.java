package com.msa.common.web.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FieldErrorResponseTest {

    @ParameterizedTest
    @ValueSource(strings = {"password", "accessToken", "clientSecret", "PASSWORD", "newPasswordConfirm", "refresh_token"})
    void 민감한_필드는_value_를_마스킹한다(String field) {
        FieldErrorResponse error = FieldErrorResponse.of(field, "raw-value", "형식이 올바르지 않습니다.");

        assertThat(error.field()).isEqualTo(field);
        assertThat(error.value()).isNull();
        assertThat(error.reason()).isEqualTo("형식이 올바르지 않습니다.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"discountRate", "name", "email"})
    void 일반_필드는_value_를_그대로_둔다(String field) {
        FieldErrorResponse error = FieldErrorResponse.of(field, 150, "할인율은 100 이하여야 합니다.");

        assertThat(error.value()).isEqualTo(150);
    }

    @Test
    void 생성자로_직접_만들어도_마스킹한다() {
        assertThat(new FieldErrorResponse("password", "1234", "필수").value()).isNull();
    }

    @Test
    void field_가_null_이어도_동작한다() {
        assertThat(FieldErrorResponse.of(null, "v", "r").value()).isEqualTo("v");
    }
}
