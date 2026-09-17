package com.backend.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

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
}
