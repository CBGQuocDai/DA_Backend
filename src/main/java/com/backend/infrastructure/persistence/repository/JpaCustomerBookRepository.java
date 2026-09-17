package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaCustomerBookEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaCustomerBookRepository extends JpaRepository<JpaCustomerBookEntity, Long> {
    List<JpaCustomerBookEntity> findByCustomerId(Long customerId);
    List<JpaCustomerBookEntity> findByBookId(Long bookId);
    Optional<JpaCustomerBookEntity> findByCustomerIdAndBookId(Long customerId, Long bookId);
}
