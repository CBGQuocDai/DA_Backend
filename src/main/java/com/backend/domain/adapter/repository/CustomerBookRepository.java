package com.backend.domain.adapter.repository;

import com.backend.domain.model.CustomerBook;

public interface CustomerBookRepository {
    boolean existsByCustomerIdAndBookId(Long customerId, Long bookId);
    CustomerBook save(CustomerBook customerBook);
}
