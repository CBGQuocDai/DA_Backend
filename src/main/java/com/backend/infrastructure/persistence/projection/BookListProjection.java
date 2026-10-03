package com.backend.infrastructure.persistence.projection;

import com.backend.domain.valueobject.BookStatus;
import java.math.BigDecimal;

public interface BookListProjection {
    Long getId();
    String getTitle();
    BigDecimal getPrice();
    String getDescription();
    String getCoverImage();
    String getContentFile();
    Long getVoiceId();
    String getVoiceName();
    String getVoiceExampleAudio();
    String getVoiceDescription();
    BookStatus getStatus();
    Double getAverageRating();
}
