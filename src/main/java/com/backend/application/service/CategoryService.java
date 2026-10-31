package com.backend.application.service;

import com.backend.application.dto.response.category.CategoryResponse;

import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getCategories();
}
