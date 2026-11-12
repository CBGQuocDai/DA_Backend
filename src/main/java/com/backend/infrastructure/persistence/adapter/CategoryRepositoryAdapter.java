package com.backend.infrastructure.persistence.adapter;

import com.backend.application.adapter.repository.CategoryRepository;
import com.backend.application.dto.response.PageResponse;
import com.backend.domain.model.Category;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.valueobject.BusinessError;
import com.backend.domain.model.CategoryStat;
import com.backend.domain.valueobject.BestsellerPeriod;
import com.backend.infrastructure.persistence.mapper.CategoryMapper;
import com.backend.infrastructure.persistence.repository.JpaBookCategoryRepository;
import com.backend.infrastructure.persistence.repository.JpaCategoryRepository;
import com.backend.infrastructure.persistence.util.LikePatterns;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryAdapter implements CategoryRepository {

    private final JpaCategoryRepository jpaCategoryRepository;
    private final JpaBookCategoryRepository jpaBookCategoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public List<Category> findAll() {
        return jpaCategoryRepository.findAll().stream()
                .map(categoryMapper::toDomain)
                .toList();
    }

    @Override
    public PageResponse<Category> search(String keyword, int page, int size) {
        return LikePatterns.toPageResponse(
                jpaCategoryRepository.search(LikePatterns.contains(keyword), PageRequest.of(page, size)),
                categoryMapper::toDomain);
    }

    @Override
    public Optional<Category> findById(Long id) {
        return jpaCategoryRepository.findById(id).map(categoryMapper::toDomain);
    }

    @Override
    public Category save(Category category) {
        return categoryMapper.toDomain(jpaCategoryRepository.save(categoryMapper.toEntity(category)));
    }

    @Override
    @Transactional
    public Category update(Category category) {
        var entity = jpaCategoryRepository.findById(category.getId())
                .orElseThrow(() -> new BusinessException(BusinessError.CATEGORY_NOT_FOUND));
        entity.setName(category.getName());
        entity.setDescription(category.getDescription());
        return categoryMapper.toDomain(jpaCategoryRepository.save(entity));
    }

    @Override
    public void deleteById(Long id) {
        jpaCategoryRepository.deleteById(id);
    }

    @Override
    public boolean isUsedByAnyBook(Long id) {
        return jpaBookCategoryRepository.existsByCategoryId(id);
    }

    @Override
    public boolean existsByName(String name, Long excludeId) {
        String trimmed = name.trim();
        return excludeId == null
                ? jpaCategoryRepository.existsByNameIgnoreCase(trimmed)
                : jpaCategoryRepository.existsByNameIgnoreCaseAndIdNot(trimmed, excludeId);
    }

    @Override
    public List<CategoryStat> findBestsellers(BestsellerPeriod.Window window) {
        return jpaCategoryRepository.findBestsellers(window.since(), window.from(), window.to()).stream()
                .<CategoryStat>map(row -> CategoryStat.builder()
                        .id(row.getId())
                        .name(row.getName())
                        .purchases(row.getPurchases())
                        .previousPurchases(window.hasPrevious() ? row.getPreviousPurchases() : null)
                        .build())
                .toList();
    }
}
