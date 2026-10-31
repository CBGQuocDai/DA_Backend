package com.backend.application.adapter.repository;

import com.backend.domain.model.CustomerBook;
import java.util.List;

public interface CustomerBookRepository {
    boolean existsByCustomerIdAndBookId(Long customerId, Long bookId);
    CustomerBook save(CustomerBook customerBook);
    List<CustomerBook> findByCustomerId(Long customerId);
}
