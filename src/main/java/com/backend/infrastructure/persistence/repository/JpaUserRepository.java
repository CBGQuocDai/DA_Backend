package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaUserRepository extends JpaRepository<JpaUserEntity, Long> {
    Optional<JpaUserEntity> findByEmail(String email);
}
