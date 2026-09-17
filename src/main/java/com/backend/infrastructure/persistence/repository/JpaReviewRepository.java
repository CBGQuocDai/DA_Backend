package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaReviewRepository extends JpaRepository<JpaReviewEntity, Long> {
}
