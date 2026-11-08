package com.backend.application.adapter.repository;

import com.backend.domain.model.CustomerBook;

import java.time.Instant;
import java.util.List;

public interface CustomerBookRepository {
    boolean existsByCustomerIdAndBookId(Long customerId, Long bookId);
    CustomerBook save(CustomerBook customerBook);
    List<CustomerBook> findByCustomerId(Long customerId);

    /** Books that became owned (paid or free) in [from, to). */
    long countPurchases(Instant from, Instant to);

    /** Customers who own at least one book. */
    long countBuyers();
}
