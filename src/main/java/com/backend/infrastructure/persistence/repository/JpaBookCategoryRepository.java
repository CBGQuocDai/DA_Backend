package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaBookCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface JpaBookCategoryRepository extends JpaRepository<JpaBookCategoryEntity, Long> {
    @Query("""
            SELECT bc FROM JpaBookCategoryEntity bc
            JOIN FETCH bc.category
            WHERE bc.bookId = :bookId
            """)
    List<JpaBookCategoryEntity> findWithCategoryByBookId(@Param("bookId") Long bookId);
    List<JpaBookCategoryEntity> findByBookIdIn(Collection<Long> bookIds);
    void deleteByBookId(Long bookId);
}
