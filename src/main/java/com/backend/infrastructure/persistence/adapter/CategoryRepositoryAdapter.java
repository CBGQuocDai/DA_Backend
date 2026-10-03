package com.backend.infrastructure.persistence.adapter;

import com.backend.domain.adapter.repository.CategoryRepository;
import com.backend.domain.mapper.CategoryMapper;
import com.backend.domain.model.Category;
import com.backend.infrastructure.persistence.repository.JpaCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryAdapter implements CategoryRepository {

    private final JpaCategoryRepository jpaCategoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public List<Category> findAll() {
        return jpaCategoryRepository.findAll().stream()
                .map(categoryMapper::toDomain)
                .toList();
    }
}
