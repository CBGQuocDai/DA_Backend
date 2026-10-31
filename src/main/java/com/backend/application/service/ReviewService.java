package com.backend.application.service;

import com.backend.application.dto.request.review.ReviewCreateRequest;
import com.backend.application.dto.response.review.ReviewCreateResponse;
import com.backend.application.dto.response.review.ReviewResponse;
import java.util.List;

public interface ReviewService {

    ReviewCreateResponse save(Long userId, ReviewCreateRequest request);

    List<ReviewResponse> getReviewByBookId(Long bookId);
}
