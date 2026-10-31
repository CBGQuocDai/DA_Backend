package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaListenProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaListenProgressRepository extends JpaRepository<JpaListenProgressEntity, Long> {
    void deleteByBookId(Long bookId);
}
