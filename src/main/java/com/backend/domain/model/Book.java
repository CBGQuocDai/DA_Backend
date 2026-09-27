package com.backend.domain.model;

import com.backend.domain.valueobject.BookStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
public class Book {
    private Long id;
    private String title;
    private Boolean isPublish;
    private BigDecimal price;
    private String description;
    private String coverImage;
    private Voice voice;
    private BookStatus status;
    private List<BookCategory> bookCategories;
    private List<BookAuthor> bookAuthors;
    private List<Chapter> chapters;
}
