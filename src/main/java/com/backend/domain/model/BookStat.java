package com.backend.domain.model;


import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookStat extends Book {
    private Double averageRating;
    private Long ratingCount;
}
