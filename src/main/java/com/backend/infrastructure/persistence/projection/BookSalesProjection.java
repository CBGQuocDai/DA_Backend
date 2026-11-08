package com.backend.infrastructure.persistence.projection;

import java.math.BigDecimal;

/** A book with its purchases in a window and in the window before it. */
public interface BookSalesProjection {
    Long getId();
    String getTitle();
    BigDecimal getPrice();
    String getCoverImage();
    Long getPurchases();
    Long getPreviousPurchases();
}
