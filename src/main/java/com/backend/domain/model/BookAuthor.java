package com.backend.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BookAuthor {
    private Long id;
    private Book book;
    private Author author;
}
