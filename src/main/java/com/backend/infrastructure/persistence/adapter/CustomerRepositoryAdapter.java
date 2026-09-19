package com.backend.infrastructure.persistence.adapter;

import com.backend.domain.mapper.CustomerMapper;
import com.backend.domain.model.Customer;
import com.backend.domain.adapter.repository.CustomerRepository;
import com.backend.infrastructure.persistence.repository.JpaCustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CustomerRepositoryAdapter implements CustomerRepository {

    private final JpaCustomerRepository jpaCustomerRepository;
    private final CustomerMapper customerMapper;

    @Override
    public Optional<Customer> findByEmail(String email) {
        return jpaCustomerRepository.findByEmail(email)
                .map(customerMapper::toDomain);
    }
}
