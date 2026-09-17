package com.backend.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Chapter {
    private Long id;
    private String title;
    private Integer chapterOrder;
    private Integer duration;
    private String audioUrl;
    private String textContext;
    private Book book;
    private AttachFile attachFile;
}
