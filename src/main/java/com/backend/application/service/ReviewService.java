package com.backend.application.service;

import com.backend.application.dto.request.review.ReviewCreateRequest;
import com.backend.application.dto.response.PageResponse;
import com.backend.application.dto.response.review.MyReviewResponse;
import com.backend.application.dto.response.review.ReviewSummaryResponse;
import com.backend.application.dto.response.review.ReviewResponse;
import java.util.List;

public interface ReviewService {

    ReviewResponse save(Long userId, ReviewCreateRequest request);

    List<ReviewResponse> getReviewByBookId(Long bookId);
    PageResponse<ReviewResponse> getPage(Long bookId, Integer rating, String sort, int page, int size);
    ReviewSummaryResponse getSummary(Long bookId);
    MyReviewResponse getMyReview(Long bookId, Long customerId);
}
