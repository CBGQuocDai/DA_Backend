package com.backend.application;

import com.backend.domain.model.Category;

import java.util.List;

public interface CategoryService {
    List<Category> getCategories();
}
