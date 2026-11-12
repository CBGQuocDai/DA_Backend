package com.backend.infrastructure.persistence.util;

import org.springframework.data.domain.Page;
import com.backend.application.dto.response.PageResponse;

import java.util.Locale;
import java.util.function.Function;

/** Tiện ích lọc chuỗi chứa từ khóa, không phân biệt hoa thường, cho các danh sách quản trị. */
public final class LikePatterns {

    private LikePatterns() {
    }

    /**
     * Chuyển từ khóa sang chữ thường, thoát ký tự đại diện LIKE bằng '\' và bọc trong '%'.
     * Từ khóa trống trở thành "%", khớp với mọi bản ghi.
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

    /** Chuyển trang dữ liệu JPA sang PageResponse bằng hàm ánh xạ được cung cấp. */
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
