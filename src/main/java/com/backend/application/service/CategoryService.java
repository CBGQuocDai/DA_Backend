package com.backend.application.service;

import com.backend.application.dto.request.category.CategoryRequest;
import com.backend.application.dto.request.statistics.BestsellerPeriod;
import com.backend.application.dto.response.PageResponse;
import com.backend.application.dto.response.category.CategoryResponse;

import java.util.List;

public interface CategoryService {
    /** Every category, for the store tabs and pickers such as the book form. */
    List<CategoryResponse> getCategories();

    /** Admin list: filtered by name or description, newest first. */
    PageResponse<CategoryResponse> searchCategories(String keyword, int page, int size);

    CategoryResponse getCategory(Long categoryId);

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(Long categoryId, CategoryRequest request);

    /** Refused while any book is still in the category. */
    void deleteCategory(Long categoryId);

    /** Admin dashboard: categories by purchases of their books in the period, each with the period before it. */
    List<CategoryResponse> getBestsellers(BestsellerPeriod period);
}
