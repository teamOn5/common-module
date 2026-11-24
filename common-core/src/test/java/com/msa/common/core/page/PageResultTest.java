package com.msa.common.core.page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.msa.common.core.error.BusinessException;
import com.msa.common.core.error.CommonErrorCode;

class PageResultTest {

    @Test
    void 데이터가_0건이면_전체_페이지_0_다음_없음() {
        PageResult<String> result = PageResult.of(List.of(), 0, 20, 0);

        assertThat(result.totalPages()).isZero();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.content()).isEmpty();
    }

    @Test
    void 딱_나누어떨어지면_나머지_페이지가_없다() {
        PageResult<String> result = PageResult.of(List.of("a"), 0, 20, 40);

        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void 나머지가_있으면_페이지가_하나_더_생긴다() {
        PageResult<String> result = PageResult.of(List.of("a"), 0, 20, 41);

        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.hasNext()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "1, 40, 2",
            "2, 41, 3",
            "5, 41, 3"
    })
    void 마지막_페이지_이후는_다음_페이지가_없다(int page, long totalElements, int expectedTotalPages) {
        PageResult<String> result = PageResult.of(List.of(), page, 20, totalElements);

        assertThat(result.totalPages()).isEqualTo(expectedTotalPages);
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void 입력값을_그대로_보존한다() {
        PageResult<String> result = PageResult.of(List.of("a", "b"), 1, 2, 5);

        assertThat(result.content()).containsExactly("a", "b");
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(5);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void 음수_page_는_예외() {
        assertThatThrownBy(() -> PageResult.of(List.of(), -1, 20, 0))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(CommonErrorCode.INVALID_INPUT);
    }

    @ParameterizedTest
    @CsvSource({"0", "-1"})
    void size_가_0_이하면_예외(int size) {
        assertThatThrownBy(() -> PageResult.of(List.of(), 0, size, 0))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(CommonErrorCode.INVALID_INPUT);
    }

    @Test
    void 음수_totalElements_는_예외() {
        assertThatThrownBy(() -> PageResult.of(List.of(), 0, 20, -1))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(CommonErrorCode.INVALID_INPUT);
    }

    @Test
    void 생성자로_직접_만들어도_검증한다() {
        assertThatThrownBy(() -> new PageResult<>(List.of(), -1, 20, 0, 0, false))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void content_가_null_이면_빈_리스트() {
        PageResult<String> result = PageResult.of(null, 0, 20, 0);

        assertThat(result.content()).isNotNull().isEmpty();
    }

    @Test
    void content_는_불변_복사본이다() {
        List<String> source = new ArrayList<>(List.of("a"));

        PageResult<String> result = PageResult.of(source, 0, 20, 1);
        source.add("b");

        assertThat(result.content()).containsExactly("a");
        assertThatThrownBy(() -> result.content().add("c"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void map_은_content_만_변환하고_페이지_정보는_유지한다() {
        PageResult<String> source = PageResult.of(List.of("a", "bb"), 0, 2, 5);

        PageResult<Integer> mapped = source.map(String::length);

        assertThat(mapped.content()).containsExactly(1, 2);
        assertThat(mapped.page()).isEqualTo(source.page());
        assertThat(mapped.size()).isEqualTo(source.size());
        assertThat(mapped.totalElements()).isEqualTo(source.totalElements());
        assertThat(mapped.totalPages()).isEqualTo(source.totalPages());
        assertThat(mapped.hasNext()).isEqualTo(source.hasNext());
    }
}
