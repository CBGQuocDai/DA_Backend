package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaBookAuthorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaBookAuthorRepository extends JpaRepository<JpaBookAuthorEntity, Long> {
    List<JpaBookAuthorEntity> findByBookId(Long bookId);
    List<JpaBookAuthorEntity> findByAuthorId(Long authorId);
    Optional<JpaBookAuthorEntity> findByBookIdAndAuthorId(Long bookId, Long authorId);
}
