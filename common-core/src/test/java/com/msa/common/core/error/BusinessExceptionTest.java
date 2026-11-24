package com.msa.common.core.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class BusinessExceptionTest {

    @Test
    void detail_이_없으면_에러코드_메시지를_반환한다() {
        BusinessException exception = new BusinessException(CommonErrorCode.NOT_FOUND);

        assertThat(exception.getMessage()).isEqualTo(CommonErrorCode.NOT_FOUND.message());
        assertThat(exception.getErrorCode()).isSameAs(CommonErrorCode.NOT_FOUND);
    }

    @Test
    void detail_이_있으면_detail_을_반환한다() {
        BusinessException exception = new BusinessException(CommonErrorCode.NOT_FOUND, "couponId=42");

        assertThat(exception.getMessage()).isEqualTo("couponId=42");
        assertThat(exception.getErrorCode()).isSameAs(CommonErrorCode.NOT_FOUND);
        assertThat(exception.getErrorCode().message()).isEqualTo(CommonErrorCode.NOT_FOUND.message());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void detail_이_비어있으면_에러코드_메시지를_반환한다(String detail) {
        BusinessException exception = new BusinessException(CommonErrorCode.INVALID_INPUT, detail);

        assertThat(exception.getMessage()).isEqualTo(CommonErrorCode.INVALID_INPUT.message());
    }

    @Test
    void cause_를_보존한다() {
        IllegalArgumentException cause = new IllegalArgumentException("원인");

        BusinessException exception = new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR, cause);

        assertThat(exception.getCause()).isSameAs(cause);
        assertThat(exception.getMessage()).isEqualTo(CommonErrorCode.EXTERNAL_SERVICE_ERROR.message());
        assertThat(exception.getErrorCode()).isSameAs(CommonErrorCode.EXTERNAL_SERVICE_ERROR);
    }

    @Test
    void errorCode_가_null_이면_예외() {
        assertThatThrownBy(() -> new BusinessException(null))
                .isInstanceOf(NullPointerException.class);
    }
}
