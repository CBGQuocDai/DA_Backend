package com.backend.infrastructure.persistence.projection;

public interface ReviewSummaryProjection {
    long getTotalReviews();
    Double getAverageRating();
    Long getOneStar();
    Long getTwoStars();
    Long getThreeStars();
    Long getFourStars();
    Long getFiveStars();
}
