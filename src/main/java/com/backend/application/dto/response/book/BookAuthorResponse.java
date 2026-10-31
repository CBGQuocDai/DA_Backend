package com.backend.application.dto.response.book;

import com.backend.application.dto.response.author.AuthorResponse;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookAuthorResponse {
    private Long id;
    private AuthorResponse author;
}
