package com.backend.infrastructure.persistence.repository;

import com.backend.domain.valueobject.PaymentStatus;
import com.backend.infrastructure.persistence.entity.JpaPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaPaymentRepository extends JpaRepository<JpaPaymentEntity, Long> {

    Optional<JpaPaymentEntity> findByPaymentCode(String paymentCode);

    Optional<JpaPaymentEntity> findFirstByCustomerIdAndBookIdAndStatusOrderByCreatedAtDesc(Long customerId, Long bookId, PaymentStatus status);
}
