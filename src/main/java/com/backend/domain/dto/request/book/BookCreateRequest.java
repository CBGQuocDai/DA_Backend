package com.backend.domain.dto.request.book;

import com.backend.domain.model.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class BookCreateRequest {
    private String title;
    private BigDecimal price;
    private String description;
    private Voice voice;
    private List<Category> categories;
    private List<Author> authors;
}
