package com.msa.common.core.error;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

class ErrorCodeValidatorTest {

    record TestErrorCode(HttpStatus status, String code, String message) implements ErrorCode {
    }

    private static ErrorCode[] codes(ErrorCode... codes) {
        return codes;
    }

    @Test
    void 규칙을_지키면_통과한다() {
        ErrorCode[] codes = codes(
                new TestErrorCode(HttpStatus.NOT_FOUND, "COUPON4040", "쿠폰을 찾을 수 없습니다."),
                new TestErrorCode(HttpStatus.NOT_FOUND, "COUPON4041", "만료된 쿠폰입니다."),
                new TestErrorCode(HttpStatus.CONFLICT, "COUPON4090", "이미 사용된 쿠폰입니다."));

        assertThatCode(() -> ErrorCodeValidator.validate(codes)).doesNotThrowAnyException();
    }

    @Test
    void 빈_배열은_통과한다() {
        assertThatCode(() -> ErrorCodeValidator.validate(new ErrorCode[0])).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"coupon4040", "COUPON404", "COUPON40400", "COUPON_4040", "4040", "COUPON 4040", ""})
    void code_형식이_틀리면_예외(String code) {
        ErrorCode[] codes = codes(new TestErrorCode(HttpStatus.NOT_FOUND, code, "메시지"));

        assertThatThrownBy(() -> ErrorCodeValidator.validate(codes))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("형식");
    }

    @Test
    void code_가_null_이면_예외() {
        ErrorCode[] codes = codes(new TestErrorCode(HttpStatus.NOT_FOUND, null, "메시지"));

        assertThatThrownBy(() -> ErrorCodeValidator.validate(codes))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("형식");
    }

    @Test
    void code_의_상태값과_status_가_다르면_예외() {
        ErrorCode[] codes = codes(new TestErrorCode(HttpStatus.NOT_FOUND, "COUPON4000", "메시지"));

        assertThatThrownBy(() -> ErrorCodeValidator.validate(codes))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COUPON4000")
                .hasMessageContaining("다릅니다");
    }

    @ParameterizedTest
    @EnumSource(value = HttpStatus.class, names = {"OK", "CONTINUE", "FOUND"})
    void status_가_400_599_범위를_벗어나면_예외(HttpStatus status) {
        ErrorCode[] codes = codes(new TestErrorCode(status, "COUPON" + status.value() + "0", "메시지"));

        assertThatThrownBy(() -> ErrorCodeValidator.validate(codes))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("범위");
    }

    @Test
    void status_가_null_이면_예외() {
        ErrorCode[] codes = codes(new TestErrorCode(null, "COUPON4040", "메시지"));

        assertThatThrownBy(() -> ErrorCodeValidator.validate(codes))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("null");
    }

    @Test
    void code_가_중복되면_예외() {
        ErrorCode[] codes = codes(
                new TestErrorCode(HttpStatus.NOT_FOUND, "COUPON4040", "쿠폰을 찾을 수 없습니다."),
                new TestErrorCode(HttpStatus.NOT_FOUND, "COUPON4040", "다른 메시지"));

        assertThatThrownBy(() -> ErrorCodeValidator.validate(codes))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("중복");
    }

    @Test
    void enum_이면_예외_메시지에_상수명이_들어간다() {
        enum BrokenErrorCode implements ErrorCode {
            WRONG(HttpStatus.NOT_FOUND, "BROKEN4000");

            private final HttpStatus status;
            private final String code;

            BrokenErrorCode(HttpStatus status, String code) {
                this.status = status;
                this.code = code;
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
                return "메시지";
            }
        }

        assertThatThrownBy(() -> ErrorCodeValidator.validate(BrokenErrorCode.values()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("BrokenErrorCode.WRONG");
    }
}
