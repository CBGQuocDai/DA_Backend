package com.backend.domain.model;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class BookStat extends Book {
    private Double averageRating;
    private Long ratingCount;
    /** Sales in a period (bestseller lists only): books that became owned, paid or free. */
    private Long purchases;
    /** Purchases in the period right before; null when there is none to compare with. */
    private Long previousPurchases;
}
