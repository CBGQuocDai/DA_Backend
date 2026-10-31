package com.backend.infrastructure.persistence.adapter;

import com.backend.application.adapter.repository.PaymentRepository;
import com.backend.domain.model.Payment;
import com.backend.domain.valueobject.PaymentStatus;
import com.backend.infrastructure.persistence.mapper.PaymentMapper;
import com.backend.infrastructure.persistence.repository.JpaPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final JpaPaymentRepository jpaPaymentRepository;
    private final PaymentMapper paymentMapper;

    @Override
    public Optional<Payment> findById(Long id) {
        return jpaPaymentRepository.findById(id).map(paymentMapper::toDomain);
    }

    @Override
    public Optional<Payment> findByPaymentCode(String paymentCode) {
        return jpaPaymentRepository.findByPaymentCode(paymentCode).map(paymentMapper::toDomain);
    }

    @Override
    public Optional<Payment> findLatestByCustomerIdAndBookIdAndStatus(Long customerId, Long bookId,
                                                                      PaymentStatus status) {
        return jpaPaymentRepository
                .findFirstByCustomerIdAndBookIdAndStatusOrderByCreatedAtDesc(customerId, bookId, status)
                .map(paymentMapper::toDomain);
    }

    @Override
    public Payment save(Payment payment) {
        return paymentMapper.toDomain(jpaPaymentRepository.save(paymentMapper.toEntity(payment)));
    }
}
