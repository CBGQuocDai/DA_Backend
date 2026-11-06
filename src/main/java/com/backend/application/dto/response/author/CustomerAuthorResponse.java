package com.backend.application.dto.response.author;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomerAuthorResponse {
    private Long id;
    private String fullName;
    private String avatar;
    private String bio;
    private String specialty;
    private Long bookCount;
}
