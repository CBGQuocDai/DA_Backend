package com.backend.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ListenProgress {
    private Long id;
    private Boolean isComplete;
    private Integer lastPlayAt;
    private Customer customer;
    private Book book;
    private Chapter chapter;
}
