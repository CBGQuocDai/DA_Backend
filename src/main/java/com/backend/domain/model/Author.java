package com.backend.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Author {
    private Long id;
    private String fullName;
    private String avatar;
    private String bio;
}
