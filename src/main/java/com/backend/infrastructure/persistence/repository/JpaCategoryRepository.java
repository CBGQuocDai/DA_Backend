package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaCategoryRepository extends JpaRepository<JpaCategoryEntity, Long> {
}
