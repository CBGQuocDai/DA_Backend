package com.backend.domain.adapter.repository;

import com.backend.domain.model.Customer;

import java.util.Optional;

public interface CustomerRepository {
    Optional<Customer> findByEmail(String email);
    Customer save(Customer customer);
    boolean existsByEmail(String email);
    Optional<Customer> findById(Long id);
}
