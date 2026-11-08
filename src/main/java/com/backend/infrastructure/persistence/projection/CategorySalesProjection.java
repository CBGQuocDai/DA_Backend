package com.backend.infrastructure.persistence.projection;

/** A category with the purchases of its books in a window and in the window before it. */
public interface CategorySalesProjection {
    Long getId();
    String getName();
    Long getPurchases();
    Long getPreviousPurchases();
}
