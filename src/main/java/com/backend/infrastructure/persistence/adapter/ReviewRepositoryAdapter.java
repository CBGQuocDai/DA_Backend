package com.backend.infrastructure.persistence.adapter;

import com.backend.application.adapter.repository.ReviewRepository;
import com.backend.infrastructure.persistence.mapper.ReviewMapper;
import com.backend.domain.model.Review;
import com.backend.infrastructure.persistence.entity.JpaReviewEntity;
import com.backend.infrastructure.persistence.repository.JpaReviewRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReviewRepositoryAdapter implements ReviewRepository {

    private final JpaReviewRepository jpaReviewRepository;
    private final ReviewMapper reviewMapper;

    @Override
    public Review save(Review review) {
        JpaReviewEntity saved = jpaReviewRepository.save(reviewMapper.toEntity(review));
        return reviewMapper.toDomain(saved);
    }

    @Override
    public List<Review> getReviewByBookId(Long bookId) {
        return jpaReviewRepository.findByBookId(bookId).stream()
            .map(reviewMapper::toDomain).toList();
    }
}
