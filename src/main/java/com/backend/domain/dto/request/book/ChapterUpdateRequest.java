package com.backend.domain.dto.request.book;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ChapterUpdateRequest {
    private String title;
    private String rawText;
    private Integer chapterOrder;
}
