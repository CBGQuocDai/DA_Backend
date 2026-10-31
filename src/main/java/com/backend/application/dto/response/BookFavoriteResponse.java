package com.backend.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookFavoriteResponse {

    private Long id;
    private Long customerId;
    private Long bookId;
    private Boolean isFavorite;
}
