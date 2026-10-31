package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaCustomerBookEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaCustomerBookRepository extends JpaRepository<JpaCustomerBookEntity, Long> {
    List<JpaCustomerBookEntity> findByCustomerId(Long customerId);
    void deleteByBookId(Long bookId);
    boolean existsByCustomerIdAndBookId(Long customerId, Long bookId);
}
