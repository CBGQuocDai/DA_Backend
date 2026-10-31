package com.backend.application.dto.response.book;

import com.backend.application.dto.response.category.CategoryResponse;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookCategoryResponse {
    private Long id;
    private CategoryResponse category;
}
