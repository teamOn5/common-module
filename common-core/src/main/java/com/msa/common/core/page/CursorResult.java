package com.msa.common.core.page;

import java.util.List;
import java.util.function.Function;

public record CursorResult<T>(
        List<T> content,
        String nextCursor,
        boolean hasNext
) {

    public CursorResult {
        content = content == null ? List.of() : List.copyOf(content);
    }

    public static <T> CursorResult<T> of(List<T> content, String nextCursor) {
        return new CursorResult<>(content, nextCursor, nextCursor != null);
    }

    public <R> CursorResult<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = content.stream().<R>map(mapper).toList();
        return new CursorResult<>(mapped, nextCursor, hasNext);
    }
}
