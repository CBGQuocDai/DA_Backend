package com.backend.domain.model;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerBook {
    private Long id;
    private Instant purchaseAt;
    private Boolean isReadComplete;
    private Customer customer;
    private Book book;
}
