package com.backend.domain.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A category with its sales in a period: purchases of books in it, paid or free. */
@Getter
@Setter
@SuperBuilder
public class CategoryStat extends Category {
    private Long purchases;
    /** Purchases in the period right before; null when there is none to compare with. */
    private Long previousPurchases;
}
