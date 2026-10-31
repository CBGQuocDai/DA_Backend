package com.backend.application.adapter.repository;

import com.backend.domain.model.Review;
import java.util.List;

public interface ReviewRepository {

    Review save(Review review);

    List<Review> getReviewByBookId(Long bookId);
}
