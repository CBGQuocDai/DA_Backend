package com.backend.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BookFavorite {
    private Long id;
    private Customer customer;
    private Book book;
}
