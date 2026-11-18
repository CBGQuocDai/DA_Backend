package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaAdminEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaAdminRepository extends JpaRepository<JpaAdminEntity, Long> {
    Optional<JpaAdminEntity> findByEmail(String email);
}
