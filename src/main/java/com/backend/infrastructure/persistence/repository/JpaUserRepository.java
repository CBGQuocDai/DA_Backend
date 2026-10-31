package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaUserRepository extends JpaRepository<JpaUserEntity, Long> {
    boolean existsByEmail(String email);
}
