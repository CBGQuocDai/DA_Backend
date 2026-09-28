package com.backend.application;

import com.backend.domain.dto.request.review.ReviewCreateRequest;
import com.backend.domain.dto.response.review.ReviewCreateResponse;
import com.backend.domain.model.Review;
import java.util.List;

public interface ReviewService {

    ReviewCreateResponse save(Long userId, ReviewCreateRequest request);

    List<Review> getReviewByBookId(Long bookId);

}
