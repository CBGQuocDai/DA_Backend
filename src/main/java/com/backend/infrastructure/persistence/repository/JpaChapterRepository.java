package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaChapterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaChapterRepository extends JpaRepository<JpaChapterEntity, Long> {
}
