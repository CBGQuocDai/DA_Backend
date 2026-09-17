package com.backend.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
public class CustomerBook {
    private Long id;
    private Instant purchaseAt;
    private Boolean isReadComplete;
    private Customer customer;
    private Book book;
}
