package com.backend.domain.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.experimental.SuperBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@SuperBuilder
public class BookStat extends Book {
    private Double averageRating;
    private Long ratingCount;
}
