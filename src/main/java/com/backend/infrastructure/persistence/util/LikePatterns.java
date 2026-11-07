package com.backend.infrastructure.persistence.util;

import org.springframework.data.domain.Page;
import com.backend.application.dto.response.PageResponse;

import java.util.Locale;
import java.util.function.Function;

/** Helpers for the "contains, case-insensitive" filters used by admin list screens. */
public final class LikePatterns {

    private LikePatterns() {
    }

    /**
     * Lower-cases the keyword, escapes LIKE wildcards with '\' and wraps it in '%'.
     * A blank keyword becomes "%", which matches every row.
     */
    public static String contains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "%";
        }
        String escaped = keyword.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    public static <E, D> PageResponse<D> toPageResponse(Page<E> page, Function<E, D> mapper) {
        return PageResponse.<D>builder()
                .page(page.getNumber())
                .pageSize(page.getSize())
                .total((int) page.getTotalElements())
                .totalPage(page.getTotalPages())
                .content(page.getContent().stream().map(mapper).toList())
                .build();
    }
}
