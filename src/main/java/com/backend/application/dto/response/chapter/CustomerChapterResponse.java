package com.backend.application.dto.response.chapter;

import com.backend.domain.valueobject.ChapterStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomerChapterResponse {
    private Long id;
    private String title;
    private Integer chapterOrder;
    private Integer duration;
    private String audioUrl;
    private ChapterStatus status;
}
