package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaListenProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaListenProgressRepository extends JpaRepository<JpaListenProgressEntity, Long> {
    List<JpaListenProgressEntity> findByCustomerId(Long customerId);
    Optional<JpaListenProgressEntity> findByCustomerIdAndBookId(Long customerId, Long bookId);
}
