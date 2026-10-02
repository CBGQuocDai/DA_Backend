package com.backend.domain.model;

import com.backend.domain.valueobject.BookStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@SuperBuilder
public class Book {
    private Long id;
    private String title;
    private BigDecimal price;
    private String description;
    private String coverImage;
    private String contentFile;
    private Voice voice;
    private BookStatus status;
    private List<BookCategory> categories;
    private List<BookAuthor> authors;
    private List<Chapter> chapters;

    public Book() {
    }
}
