package com.backend.application.adapter.repository;

import com.backend.application.dto.response.PageResponse;
import com.backend.domain.model.Author;

import java.util.List;
import java.util.Optional;

public interface AuthorRepository {
    /** Lấy danh sách toàn bộ tác giả. */
    List<Author> findAll();

    /** Tìm tên tác giả không phân biệt dấu, sắp xếp A–Z; từ khóa trống lấy tất cả. */
    PageResponse<Author> searchByName(String keyword, int page, int size);

    /** Tìm tác giả theo ID; trả về Optional rỗng nếu không tồn tại. */
    Optional<Author> findById(Long id);

    /** Lưu dữ liệu và trả về tác giả đã lưu. */
    Author save(Author author);

    /** Xóa tác giả theo ID. */
    void deleteById(Long id);

    /** Kiểm tra tác giả có đang được sách nào sử dụng hay không. */
    boolean isUsedByAnyBook(Long id);
}
