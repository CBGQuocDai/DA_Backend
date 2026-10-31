package com.backend.application.dto.response.author;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorResponse {
    private Long id;
    private String fullName;
    private String avatar;
    private String bio;
}
