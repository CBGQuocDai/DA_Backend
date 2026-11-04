package com.backend.infrastructure.persistence.projection;

import com.backend.domain.valueobject.ChapterStatus;

/** Select only fields needed by the customer chapter cards. */
public interface CustomerChapterProjection {
    Long getId();
    String getTitle();
    Integer getChapterOrder();
    Integer getDuration();
    String getAudioUrl();
    ChapterStatus getStatus();
}
