package com.backend.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
public class Review {
    private Long id;
    private Double rate;
    private String content;
    private Instant createAt;
    private Customer customer;
    private Book book;
    /** Read-model flag computed from the reviewer's actual book ownership. */
    private boolean verifiedPurchase;
}
