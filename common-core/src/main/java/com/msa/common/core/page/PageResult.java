package com.msa.common.core.page;

import java.util.List;
import java.util.function.Function;

import com.msa.common.core.error.BusinessException;
import com.msa.common.core.error.CommonErrorCode;

public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public PageResult {
        if (page < 0) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "page 는 0 이상이어야 합니다. page=" + page);
        }
        if (size <= 0) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "size 는 1 이상이어야 합니다. size=" + size);
        }
        if (totalElements < 0) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT,
                    "totalElements 는 0 이상이어야 합니다. totalElements=" + totalElements);
        }
        content = content == null ? List.of() : List.copyOf(content);
    }

    public static <T> PageResult<T> of(List<T> content, int page, int size, long totalElements) {
        if (size <= 0) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "size 는 1 이상이어야 합니다. size=" + size);
        }
        int totalPages = totalElements <= 0 ? 0 : Math.toIntExact((totalElements + size - 1) / size);
        boolean hasNext = page + 1 < totalPages;
        return new PageResult<>(content, page, size, totalElements, totalPages, hasNext);
    }

    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = content.stream().<R>map(mapper).toList();
        return new PageResult<>(mapped, page, size, totalElements, totalPages, hasNext);
    }
}
