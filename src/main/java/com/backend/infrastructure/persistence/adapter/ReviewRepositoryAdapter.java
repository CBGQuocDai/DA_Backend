package com.backend.infrastructure.persistence.adapter;

import com.backend.application.adapter.repository.ReviewRepository;
import com.backend.application.dto.response.PageResponse;
import com.backend.application.dto.response.review.ReviewSummaryResponse;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Book;
import com.backend.domain.model.Customer;
import com.backend.domain.model.Review;
import com.backend.domain.valueobject.BusinessError;
import com.backend.infrastructure.persistence.entity.JpaReviewEntity;
import com.backend.infrastructure.persistence.mapper.ReviewMapper;
import com.backend.infrastructure.persistence.projection.ReviewListProjection;
import com.backend.infrastructure.persistence.projection.ReviewSummaryProjection;
import com.backend.infrastructure.persistence.repository.JpaReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReviewRepositoryAdapter implements ReviewRepository {

    private final JpaReviewRepository jpaReviewRepository;
    private final ReviewMapper reviewMapper;

    @Override
    public Review save(Review review) {
        try {
            JpaReviewEntity saved = jpaReviewRepository.saveAndFlush(reviewMapper.toEntity(review));
            return reviewMapper.toDomain(saved);
        } catch (DataIntegrityViolationException error) {
            if (String.valueOf(error.getMostSpecificCause().getMessage()).toLowerCase(Locale.ROOT)
                    .contains("uk_review_customer_book")) {
                throw new BusinessException(BusinessError.REVIEW_ALREADY_EXISTS);
            }
            throw error;
        }
    }

    @Override
    public List<Review> getReviewByBookId(Long bookId) {
        return jpaReviewRepository.findReviewPage(bookId, null, Pageable.unpaged(reviewSort("newest")))
                .getContent().stream().map(this::toDomain).toList();
    }

    @Override
    public PageResponse<Review> findPage(Long bookId, Integer rating, String sort, int page, int size) {
        var result = jpaReviewRepository.findReviewPage(bookId, rating == null ? null : rating.doubleValue(),
                PageRequest.of(page, size, reviewSort(sort)));
        return PageResponse.<Review>builder()
                .content(result.getContent().stream().map(this::toDomain).toList())
                .total(Math.toIntExact(result.getTotalElements())).totalPage(result.getTotalPages())
                .page(page).pageSize(size).build();
    }

    @Override
    public ReviewSummaryResponse getSummary(Long bookId) {
        return toSummary(jpaReviewRepository.summarizeByBookId(bookId));
    }

    @Override
    public ReviewSummaryResponse getOverallSummary() {
        return toSummary(jpaReviewRepository.summarizeAll());
    }

    private ReviewSummaryResponse toSummary(ReviewSummaryProjection result) {
        double average = result.getAverageRating() == null ? 0 : Math.round(result.getAverageRating() * 10) / 10.0;
        return ReviewSummaryResponse.builder().averageRating(average).totalReviews(result.getTotalReviews())
                .distribution(Map.of(1, count(result.getOneStar()), 2, count(result.getTwoStars()),
                        3, count(result.getThreeStars()), 4, count(result.getFourStars()), 5, count(result.getFiveStars())))
                .build();
    }

    @Override
    public Optional<Review> findByBookIdAndCustomerId(Long bookId, Long customerId) {
        return jpaReviewRepository.findOwnReview(bookId, customerId, PageRequest.of(0, 1, reviewSort("newest")))
                .stream().findFirst().map(this::toDomain);
    }

    @Override
    public boolean existsByBookIdAndCustomerId(Long bookId, Long customerId) {
        return jpaReviewRepository.existsByBookIdAndCustomerId(bookId, customerId);
    }

    private Sort reviewSort(String sort) {
        Sort newest = Sort.by(Sort.Order.desc("createAt"), Sort.Order.desc("id"));
        return "highest".equals(sort) ? Sort.by(Sort.Order.desc("rate")).and(newest) : newest;
    }

    private long count(Long value) { return value == null ? 0 : value; }

    private Review toDomain(ReviewListProjection row) {
        return Review.builder().id(row.getId()).book(Book.builder().id(row.getBookId()).build())
                .customer(Customer.builder().id(row.getUserId()).fullName(row.getUserName()).avatarUrl(row.getUserAvatar()).build())
                .rate(row.getRating()).content(row.getComment()).createAt(row.getCreatedAt())
                .verifiedPurchase(row.getVerifiedPurchase()).build();
    }
}
