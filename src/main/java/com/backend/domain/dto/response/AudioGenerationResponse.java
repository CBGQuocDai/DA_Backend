package com.backend.domain.dto.response;

import com.backend.domain.valueobject.ChapterStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AudioGenerationResponse {
    private Long chapterId;
    private String audioUrl;
    private Integer duration;
    private ChapterStatus status;
}
