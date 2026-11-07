package com.backend.infrastructure.persistence.projection;

import java.time.Instant;

public interface ReviewListProjection {
    Long getId();
    Long getBookId();
    Long getUserId();
    String getUserName();
    String getUserAvatar();
    Double getRating();
    String getComment();
    Instant getCreatedAt();
    boolean getVerifiedPurchase();
}
