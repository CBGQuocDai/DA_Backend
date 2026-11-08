package com.backend.infrastructure.persistence.adapter;

import com.backend.application.adapter.repository.CustomerBookRepository;
import com.backend.domain.model.CustomerBook;
import com.backend.infrastructure.persistence.mapper.CustomerBookMapper;
import com.backend.infrastructure.persistence.repository.JpaCustomerBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CustomerBookRepositoryAdapter implements CustomerBookRepository {

    private final JpaCustomerBookRepository jpaCustomerBookRepository;
    private final CustomerBookMapper customerBookMapper;

    @Override
    public boolean existsByCustomerIdAndBookId(Long customerId, Long bookId) {
        return jpaCustomerBookRepository.existsByCustomerIdAndBookId(customerId, bookId);
    }

    @Override
    public CustomerBook save(CustomerBook customerBook) {
        return customerBookMapper.toDomain(jpaCustomerBookRepository.save(customerBookMapper.toEntity(customerBook)));
    }

    @Override
    public List<CustomerBook> findByCustomerId(Long customerId) {
        return jpaCustomerBookRepository.findByCustomerId(customerId).stream()
                .map(customerBookMapper::toDomain)
                .toList();
    }

    @Override
    public long countPurchases(Instant from, Instant to) {
        return jpaCustomerBookRepository.countPurchasesBetween(from, to);
    }

    @Override
    public long countBuyers() {
        return jpaCustomerBookRepository.countDistinctCustomers();
    }

}
