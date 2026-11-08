package com.backend.application.adapter.repository;

import com.backend.application.dto.response.PageResponse;
import com.backend.domain.model.Category;
import com.backend.domain.model.CategoryStat;

import java.time.Instant;
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

    /**
     * Categories with purchases in [from, to), most purchased first; a book in several categories counts in
     * each. Purchases in [previousFrom, from) are filled in when {@code previousFrom} is not null.
     */
    List<CategoryStat> findBestsellers(Instant from, Instant to, Instant previousFrom);
}
