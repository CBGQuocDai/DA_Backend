package com.backend.infrastructure.persistence.adapter;

import com.backend.domain.adapter.repository.CustomerBookRepository;
import com.backend.domain.mapper.CustomerBookMapper;
import com.backend.domain.model.CustomerBook;
import com.backend.infrastructure.persistence.entity.JpaCustomerBookEntity;
import com.backend.infrastructure.persistence.repository.JpaCustomerBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerBookRepositoryAdapter implements CustomerBookRepository {

    private final JpaCustomerBookRepository jpaCustomerBookRepository;
    private final CustomerBookMapper customerBookMapper;

    @Override
    public boolean existsByCustomerIdAndBookId(Long customerId, Long bookId) {
        if (customerId == null || bookId == null) {
            return false;
        }
        return jpaCustomerBookRepository.existsByCustomerIdAndBookId(customerId, bookId);
    }

    @Override
    public CustomerBook save(CustomerBook customerBook) {
        JpaCustomerBookEntity entity = customerBookMapper.toEntity(customerBook);
        JpaCustomerBookEntity saved = jpaCustomerBookRepository.save(entity);
        return customerBookMapper.toDomain(saved);
    }
}
