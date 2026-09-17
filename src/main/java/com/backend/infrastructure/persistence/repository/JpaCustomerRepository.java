package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaCustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaCustomerRepository extends JpaRepository<JpaCustomerEntity, Long> {
    Optional<JpaCustomerEntity> findByEmail(String email);
}

