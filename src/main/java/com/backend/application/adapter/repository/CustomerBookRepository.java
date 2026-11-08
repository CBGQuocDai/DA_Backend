package com.backend.application.adapter.repository;

import com.backend.domain.model.CustomerBook;
import com.backend.domain.valueobject.BestsellerPeriod;
import com.backend.domain.valueobject.PeriodCount;

import java.util.List;

public interface CustomerBookRepository {
    boolean existsByCustomerIdAndBookId(Long customerId, Long bookId);
    CustomerBook save(CustomerBook customerBook);
    List<CustomerBook> findByCustomerId(Long customerId);

    /** Books that became owned (paid or free) in the window, and in the window before. */
    PeriodCount countPurchases(BestsellerPeriod.Window window);

    /** Customers who own at least one book. */
    long countBuyers();
}
