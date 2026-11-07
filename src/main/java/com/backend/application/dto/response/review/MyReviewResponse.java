package com.backend.application.dto.response.review;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyReviewResponse {
    private boolean hasReviewed;
    private ReviewResponse review;
}
