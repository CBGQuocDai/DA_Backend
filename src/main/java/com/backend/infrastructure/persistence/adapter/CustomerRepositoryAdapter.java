package com.backend.infrastructure.persistence.adapter;

import com.backend.domain.adapter.repository.CustomerRepository;
import com.backend.domain.mapper.CustomerMapper;
import com.backend.domain.model.Customer;
import com.backend.infrastructure.persistence.entity.JpaCustomerEntity;
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

    @Override
    public Customer save(Customer customer) {
        JpaCustomerEntity saved = jpaCustomerRepository.save(customerMapper.toEntity(customer));
        return customerMapper.toDomain(saved);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaCustomerRepository.existsByEmail(email);
    }

    @Override
    public Optional<Customer> findById(Long id) {
        return jpaCustomerRepository.findById(id).map(customerMapper::toDomain);
    }


}
