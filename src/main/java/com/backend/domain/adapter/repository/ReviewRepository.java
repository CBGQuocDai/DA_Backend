package com.backend.domain.adapter.repository;

import com.backend.domain.model.Customer;
import com.backend.domain.model.Review;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository {

    Review save(Review review);

    List<Review> getReviewByBookId(Long bookId);
}
