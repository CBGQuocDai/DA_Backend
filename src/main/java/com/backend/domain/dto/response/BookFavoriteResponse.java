package com.backend.domain.dto.response;

import com.backend.domain.model.Book;
import com.backend.domain.model.Customer;
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
