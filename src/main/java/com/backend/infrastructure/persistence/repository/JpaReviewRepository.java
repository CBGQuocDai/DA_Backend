package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaReviewEntity;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaReviewRepository extends JpaRepository<JpaReviewEntity, Long> {

    @EntityGraph(attributePaths = {"customer"})
    List<JpaReviewEntity> findByBookId(Long bookId);
}
