package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaBookAuthorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface JpaBookAuthorRepository extends JpaRepository<JpaBookAuthorEntity, Long> {
    @Query("""
            SELECT ba FROM JpaBookAuthorEntity ba
            JOIN FETCH ba.author
            WHERE ba.bookId = :bookId
            """)
    List<JpaBookAuthorEntity> findWithAuthorByBookId(@Param("bookId") Long bookId);

    // Lấy danh sách tác giả theo danh sách ID sách (kèm FETCH JOIN để tránh N+1)
    @Query("""
            SELECT ba FROM JpaBookAuthorEntity ba
            JOIN FETCH ba.author
            WHERE ba.bookId IN :bookIds
            ORDER BY ba.id
            """)
    List<JpaBookAuthorEntity> findWithAuthorByBookIdIn(@Param("bookIds") Collection<Long> bookIds);

    List<JpaBookAuthorEntity> findByBookIdIn(Collection<Long> bookIds);
    void deleteByBookId(Long bookId);
    boolean existsByAuthorId(Long authorId);
}
