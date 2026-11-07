package com.backend.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthorSummary {
    private Long id;
    private String fullName;
    private String avatar;
    private String bio;
    private String specialty;
    private Long bookCount;
}
