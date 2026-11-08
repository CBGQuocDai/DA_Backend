package com.backend.infrastructure.persistence.projection;

/** A category with the purchases of its books in one time window. */
public interface CategorySalesProjection {
    Long getId();
    String getName();
    String getDescription();
    Long getPurchases();
}
