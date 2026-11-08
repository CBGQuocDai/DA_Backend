package com.backend.infrastructure.persistence.projection;

import com.backend.domain.valueobject.BookStatus;

import java.math.BigDecimal;

/** A book with its purchases and paid revenue in one time window. */
public interface BookSalesProjection {
    Long getId();
    String getTitle();
    BigDecimal getPrice();
    String getCoverImage();
    BookStatus getStatus();
    Long getPurchases();
    BigDecimal getRevenue();
}
