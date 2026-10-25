package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaBookCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaBookCategoryRepository extends JpaRepository<JpaBookCategoryEntity, Long> {
    List<JpaBookCategoryEntity> findByBookId(Long bookId);

    @Query("""
            SELECT bc FROM JpaBookCategoryEntity bc
            JOIN FETCH bc.category
            WHERE bc.bookId = :bookId
            """)
    List<JpaBookCategoryEntity> findWithCategoryByBookId(@Param("bookId") Long bookId);
    List<JpaBookCategoryEntity> findByCategoryId(Long categoryId);
    List<JpaBookCategoryEntity> findByBookIdIn(Collection<Long> bookIds);
    Optional<JpaBookCategoryEntity> findByBookIdAndCategoryId(Long bookId, Long categoryId);
    void deleteByBookId(Long bookId);
}
