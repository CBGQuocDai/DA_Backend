package com.backend.infrastructure.persistence.adapter;

import com.backend.application.adapter.repository.CustomerBookRepository;
import com.backend.domain.model.CustomerBook;
import com.backend.domain.valueobject.BestsellerPeriod;
import com.backend.domain.valueobject.PeriodCount;
import com.backend.infrastructure.persistence.mapper.CustomerBookMapper;
import com.backend.infrastructure.persistence.projection.PeriodCountProjection;
import com.backend.infrastructure.persistence.repository.JpaCustomerBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
    public PeriodCount countPurchases(BestsellerPeriod.Window window) {
        PeriodCountProjection row = jpaCustomerBookRepository.countPurchases(window.since(), window.from(), window.to());
        return new PeriodCount(row.getPurchases(), window.hasPrevious() ? row.getPreviousPurchases() : null);
    }

    @Override
    public long countBuyers() {
        return jpaCustomerBookRepository.countDistinctCustomers();
    }

}
