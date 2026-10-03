package com.backend.domain.dto.response;

import com.backend.domain.model.Book;
import com.backend.domain.model.Customer;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BookFavoriteResponse {

    private Long id;
    private Long customerId;
    private Long bookId;
}
