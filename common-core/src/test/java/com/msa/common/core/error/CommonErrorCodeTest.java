package com.msa.common.core.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class CommonErrorCodeTest {

    @Test
    void 에러_코드_규칙을_지킨다() {
        assertThatCode(() -> ErrorCodeValidator.validate(CommonErrorCode.values()))
                .doesNotThrowAnyException();
    }

    @Test
    void 모든_메시지가_비어있지_않다() {
        for (CommonErrorCode errorCode : CommonErrorCode.values()) {
            assertThat(errorCode.message()).as(errorCode.name()).isNotBlank();
        }
    }

    @Test
    void 상수별_status_와_code_가_정의대로다() {
        assertThat(CommonErrorCode.INVALID_INPUT.status()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(CommonErrorCode.INVALID_INPUT.code()).isEqualTo("COMMON4000");
        assertThat(CommonErrorCode.ALREADY_DELETED.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(CommonErrorCode.ALREADY_DELETED.code()).isEqualTo("COMMON4091");
        assertThat(CommonErrorCode.EXTERNAL_SERVICE_ERROR.status()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(CommonErrorCode.EXTERNAL_SERVICE_ERROR.code()).isEqualTo("COMMON5020");
    }
}
