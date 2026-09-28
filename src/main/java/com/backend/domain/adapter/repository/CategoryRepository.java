package com.backend.domain.adapter.repository;

import com.backend.domain.model.Category;

import java.util.List;

public interface CategoryRepository {
    List<Category> findAll();
}
