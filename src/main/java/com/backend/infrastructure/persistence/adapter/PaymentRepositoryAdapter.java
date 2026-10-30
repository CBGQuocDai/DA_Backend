package com.backend.infrastructure.persistence.adapter;

import com.backend.domain.adapter.repository.PaymentRepository;
import com.backend.domain.mapper.PaymentMapper;
import com.backend.domain.model.Payment;
import com.backend.domain.valueobject.PaymentStatus;
import com.backend.infrastructure.persistence.entity.JpaPaymentEntity;
import com.backend.infrastructure.persistence.repository.JpaPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final JpaPaymentRepository jpaPaymentRepository;
    private final PaymentMapper paymentMapper;

    @Override
    public Payment save(Payment payment) {
        JpaPaymentEntity entity = paymentMapper.toEntity(payment);
        JpaPaymentEntity saved = jpaPaymentRepository.save(entity);
        return paymentMapper.toDomain(saved);
    }

    @Override
    public Optional<Payment> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return jpaPaymentRepository.findById(id).map(paymentMapper::toDomain);
    }

    @Override
    public Optional<Payment> findByPaymentCode(String paymentCode) {
        if (paymentCode == null) {
            return Optional.empty();
        }
        return jpaPaymentRepository.findByPaymentCode(paymentCode).map(paymentMapper::toDomain);
    }

    @Override
    public Optional<Payment> findFirstPendingPayment(Long customerId, Long bookId) {
        if (customerId == null || bookId == null) {
            return Optional.empty();
        }
        return jpaPaymentRepository
                .findFirstByCustomerIdAndBookIdAndStatusOrderByCreatedAtDesc(customerId, bookId, PaymentStatus.PENDING)
                .map(paymentMapper::toDomain);
    }
}
