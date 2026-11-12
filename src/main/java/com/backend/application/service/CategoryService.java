package com.backend.application.service;

import com.backend.application.dto.request.category.CategoryRequest;
import com.backend.application.dto.response.PageResponse;
import com.backend.application.dto.response.category.CategoryResponse;
import com.backend.domain.valueobject.BestsellerPeriod;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getCategories();
    PageResponse<CategoryResponse> searchCategories(String keyword, int page, int size);
    CategoryResponse createCategory(CategoryRequest request);
    CategoryResponse updateCategory(Long categoryId, CategoryRequest request);
    void deleteCategory(Long categoryId);

    List<CategoryResponse> getBestsellers(BestsellerPeriod period);
}
