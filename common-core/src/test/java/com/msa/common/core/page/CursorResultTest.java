package com.msa.common.core.page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class CursorResultTest {

    @Test
    void nextCursor_가_있으면_다음_있음() {
        CursorResult<String> result = CursorResult.of(List.of("a", "b"), "cursor-2");

        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo("cursor-2");
        assertThat(result.content()).containsExactly("a", "b");
    }

    @Test
    void nextCursor_가_null_이면_다음_없음() {
        CursorResult<String> result = CursorResult.of(List.of("a"), null);

        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    void content_가_null_이면_빈_리스트() {
        CursorResult<String> result = CursorResult.of(null, null);

        assertThat(result.content()).isNotNull().isEmpty();
    }

    @Test
    void content_는_불변이다() {
        CursorResult<String> result = CursorResult.of(List.of("a"), null);

        assertThatThrownBy(() -> result.content().add("b"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void map_은_content_만_변환하고_커서_정보는_유지한다() {
        CursorResult<String> source = CursorResult.of(List.of("a", "bb"), "cursor-2");

        CursorResult<Integer> mapped = source.map(String::length);

        assertThat(mapped.content()).containsExactly(1, 2);
        assertThat(mapped.nextCursor()).isEqualTo("cursor-2");
        assertThat(mapped.hasNext()).isTrue();
    }

    @Test
    void 다음이_없는_결과를_map_해도_다음_없음이_유지된다() {
        CursorResult<Integer> mapped = CursorResult.of(List.of("a"), null).map(String::length);

        assertThat(mapped.hasNext()).isFalse();
        assertThat(mapped.nextCursor()).isNull();
    }
}
