package com.backend.application.service.impl;

import com.backend.application.service.CategoryService;
import com.backend.application.adapter.repository.CategoryRepository;
import com.backend.application.dto.response.category.CategoryResponse;
import com.backend.application.mapper.ResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ResponseMapper responseMapper;

    @Override
    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAll().stream().map(responseMapper::toCategoryResponse).toList();
    }
}
