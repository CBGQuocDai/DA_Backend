package com.backend.application.adapter.repository;

import com.backend.application.dto.response.PageResponse;
import com.backend.domain.model.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    List<Category> findAll();

    /** Newest first; a blank keyword matches every category. Matches name or description. */
    PageResponse<Category> search(String keyword, int page, int size);

    Optional<Category> findById(Long id);

    Category save(Category category);

    void deleteById(Long id);

    boolean isUsedByAnyBook(Long id);

    /** Case-insensitive; {@code excludeId} skips the category being renamed (null on create). */
    boolean existsByName(String name, Long excludeId);
}
