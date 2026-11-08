package com.backend.infrastructure.persistence.projection;

/** Purchases of one book in a time window. */
public interface PurchaseCountProjection {
    Long getBookId();
    Long getPurchases();
}
