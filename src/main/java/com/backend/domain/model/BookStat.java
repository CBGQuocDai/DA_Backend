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
}
