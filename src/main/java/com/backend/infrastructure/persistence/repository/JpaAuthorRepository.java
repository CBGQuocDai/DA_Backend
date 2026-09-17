package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaAuthorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaAuthorRepository extends JpaRepository<JpaAuthorEntity, Long> {
}
