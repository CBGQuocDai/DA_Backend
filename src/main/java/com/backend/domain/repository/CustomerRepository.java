package com.backend.domain.repository;

import com.backend.domain.model.Customer;

import java.util.Optional;

public interface CustomerRepository {
    Optional<Customer> findByEmail(String email);

    Customer save(Customer customer);
}
