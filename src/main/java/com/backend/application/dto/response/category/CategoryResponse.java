package com.backend.application.dto.response.category;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private Long id;
    private String name;
    private String description;
    /** Bestseller lists only: purchases of books in the category in the chosen period. */
    private Long purchases;
    /** Bestseller lists only: purchases in the period before; absent when there is none to compare with. */
    private Long previousPurchases;
}
