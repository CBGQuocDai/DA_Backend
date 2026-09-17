package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaBookCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaBookCategoryRepository extends JpaRepository<JpaBookCategoryEntity, Long> {
    List<JpaBookCategoryEntity> findByBookId(Long bookId);
    List<JpaBookCategoryEntity> findByCategoryId(Long categoryId);
    Optional<JpaBookCategoryEntity> findByBookIdAndCategoryId(Long bookId, Long categoryId);
}
