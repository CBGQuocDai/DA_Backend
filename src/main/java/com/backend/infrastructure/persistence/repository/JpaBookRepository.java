package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaBookEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaBookRepository extends JpaRepository<JpaBookEntity, Long> {
}
