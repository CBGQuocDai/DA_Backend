package com.backend.infrastructure.persistence.adapter;

import com.backend.application.adapter.repository.CategoryRepository;
import com.backend.application.dto.response.PageResponse;
import com.backend.domain.model.Category;
import com.backend.domain.model.CategoryStat;
import com.backend.infrastructure.persistence.mapper.CategoryMapper;
import com.backend.infrastructure.persistence.projection.CategorySalesProjection;
import com.backend.infrastructure.persistence.repository.JpaBookCategoryRepository;
import com.backend.infrastructure.persistence.repository.JpaCategoryRepository;
import com.backend.infrastructure.persistence.util.LikePatterns;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    public List<CategoryStat> findBestsellers(Instant from, Instant to, Instant previousFrom) {
        Map<Long, Long> previous = previousFrom == null ? Map.of()
                : jpaCategoryRepository.findBestsellers(previousFrom, from).stream()
                        .collect(Collectors.toMap(CategorySalesProjection::getId, CategorySalesProjection::getPurchases));
        return jpaCategoryRepository.findBestsellers(from, to).stream()
                .<CategoryStat>map(row -> CategoryStat.builder()
                        .id(row.getId())
                        .name(row.getName())
                        .description(row.getDescription())
                        .purchases(row.getPurchases())
                        .previousPurchases(previousFrom == null ? null : previous.getOrDefault(row.getId(), 0L))
                        .build())
                .toList();
    }
}
