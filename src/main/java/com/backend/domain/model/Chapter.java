package com.backend.domain.model;

import com.backend.domain.valueobject.ChapterStatus;
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
    private String rawText;
    private ChapterStatus status;
    private Book book;
    private AttachFile attachFile;
}
