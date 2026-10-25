package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaBookAuthorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaBookAuthorRepository extends JpaRepository<JpaBookAuthorEntity, Long> {
    List<JpaBookAuthorEntity> findByBookId(Long bookId);

    @Query("""
            SELECT ba FROM JpaBookAuthorEntity ba
            JOIN FETCH ba.author
            WHERE ba.bookId = :bookId
            """)
    List<JpaBookAuthorEntity> findWithAuthorByBookId(@Param("bookId") Long bookId);
    List<JpaBookAuthorEntity> findByAuthorId(Long authorId);
    List<JpaBookAuthorEntity> findByBookIdIn(Collection<Long> bookIds);
    Optional<JpaBookAuthorEntity> findByBookIdAndAuthorId(Long bookId, Long authorId);
    void deleteByBookId(Long bookId);
}
