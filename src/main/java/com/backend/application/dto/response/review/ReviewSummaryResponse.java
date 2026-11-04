package com.backend.application.dto.response.review;

import lombok.Builder;
import lombok.Getter;
import java.util.Map;

@Getter
@Builder
public class ReviewSummaryResponse {
    private double averageRating;
    private long totalReviews;
    private Map<Integer, Long> distribution;
}
