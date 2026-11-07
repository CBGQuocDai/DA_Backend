package com.backend.application.adapter.repository;

import com.backend.domain.model.Review;
import com.backend.application.dto.response.PageResponse;
import com.backend.application.dto.response.review.ReviewSummaryResponse;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository {

    Review save(Review review);

    List<Review> getReviewByBookId(Long bookId);
    PageResponse<Review> findPage(Long bookId, Integer rating, String sort, int page, int size);
    ReviewSummaryResponse getSummary(Long bookId);
    Optional<Review> findByBookIdAndCustomerId(Long bookId, Long customerId);
    boolean existsByBookIdAndCustomerId(Long bookId, Long customerId);
}
