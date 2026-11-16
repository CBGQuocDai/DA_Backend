package com.backend.application.service.impl;

import com.backend.application.adapter.repository.CategoryRepository;
import com.backend.application.dto.request.category.CategoryRequest;
import com.backend.application.dto.response.PageResponse;
import com.backend.application.dto.response.category.CategoryResponse;
import com.backend.application.mapper.ResponseMapper;
import com.backend.application.service.CategoryService;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Category;
import com.backend.domain.valueobject.BestsellerPeriod;
import com.backend.domain.valueobject.BusinessError;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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

    @Override
    public PageResponse<CategoryResponse> searchCategories(String keyword, int page, int size) {
        return responseMapper.toPageResponse(
                categoryRepository.search(keyword, page, size), responseMapper::toCategoryResponse);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest req) {
        if (categoryRepository.existsByName(req.getName(), null)) {
            throw new BusinessException(BusinessError.CATEGORY_EXISTED);
        }
        Category category = new Category();
        applyRequest(category, req);
        return responseMapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long categoryId, CategoryRequest req) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(BusinessError.CATEGORY_NOT_FOUND));
        if (categoryRepository.existsByName(req.getName(), categoryId)) {
            throw new BusinessException(BusinessError.CATEGORY_EXISTED);
        }
        applyRequest(category, req);
        return responseMapper.toCategoryResponse(categoryRepository.update(category));
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId) {
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(BusinessError.CATEGORY_NOT_FOUND));
        if (categoryRepository.isUsedByAnyBook(categoryId)) {
            throw new BusinessException(BusinessError.CATEGORY_IN_USE);
        }
        categoryRepository.deleteById(categoryId);
    }

    private void applyRequest(Category category, CategoryRequest req) {
        category.setName(req.getName().trim());
        String description = req.getDescription();
        category.setDescription(description == null || description.isBlank() ? null : description.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getBestsellers(BestsellerPeriod period) {
        return categoryRepository.findBestsellers(period.endingAt(Instant.now()))
                .stream().map(responseMapper::toCategoryResponse).toList();
    }
}
