package com.backend.application.dto.response.chapter;

import com.backend.domain.valueobject.ChapterStatus;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterResponse {
    private Long id;
    private String title;
    private Integer chapterOrder;
    private Integer duration;
    private String audioUrl;
    private String rawText;
    private Double readingSpeed;
    private ChapterStatus status;
}
