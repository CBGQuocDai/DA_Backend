package com.backend.domain.model;

import com.backend.domain.valueobject.ChapterStatus;
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
public class Chapter {
    /** Line separating PDF pages in rawText. */
    public static final String PAGE_BREAK = "-------------page_break------------------";

    private Long id;
    private String title;
    private Integer chapterOrder;
    private Integer duration; // seconds
    private String audioUrl;
    private String rawText;
    private String contentJson; // editor document with formatting; when set, rawText is derived from it
    private Double readingSpeed;
    private ChapterStatus status;
    private AttachFile attachFile;
}
