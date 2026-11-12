package com.backend.application.service.impl;

import com.backend.application.dto.request.review.ReviewCreateRequest;
import com.backend.application.dto.response.PageResponse;
import com.backend.application.dto.response.review.MyReviewResponse;
import com.backend.application.dto.response.review.ReviewSummaryResponse;
import com.backend.application.dto.response.review.ReviewResponse;
import com.backend.application.mapper.ResponseMapper;
import com.backend.application.adapter.repository.BookRepository;
import com.backend.application.adapter.repository.CustomerRepository;
import com.backend.application.adapter.repository.CustomerBookRepository;
import com.backend.application.adapter.repository.ReviewRepository;
import com.backend.application.service.ReviewService;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Book;
import com.backend.domain.model.Customer;
import com.backend.domain.model.Review;
import com.backend.domain.valueobject.BusinessError;
import com.backend.domain.valueobject.BookStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final CustomerRepository customerRepository;
    private final CustomerBookRepository customerBookRepository;
    private final ResponseMapper responseMapper;

    @Override
    @Transactional
    public ReviewResponse save(Long userId, ReviewCreateRequest request) {
        if (userId == null) {
            throw new BusinessException(BusinessError.UNAUTHORIZED);
        }
        if (request.getBookId() == null) {
            throw new BusinessException(BusinessError.BOOK_NOT_FOUND);
        }

        Customer customer = customerRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(BusinessError.USER_NOT_FOUND));
        // The book row serializes concurrent submissions, including across app instances.
        Book book = bookRepository.findBookOverviewById(request.getBookId())
                .filter(candidate -> candidate.getStatus() == BookStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(BusinessError.BOOK_NOT_FOUND));
        if (!customerBookRepository.existsByCustomerIdAndBookId(userId, request.getBookId())) {
            throw new BusinessException(BusinessError.REVIEW_PURCHASE_REQUIRED);
        }
        if (reviewRepository.existsByBookIdAndCustomerId(request.getBookId(), userId)) {
            throw new BusinessException(BusinessError.REVIEW_ALREADY_EXISTS);
        }

        Review review = Review.builder()
            .rate(request.getRate())
            .content(request.getContent().trim())
            .customer(customer)
            .book(book)
            .build();
        Review saved = reviewRepository.save(review);

        saved.setCustomer(customer);
        saved.setBook(book);
        saved.setVerifiedPurchase(true);
        return responseMapper.toReviewResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewByBookId(Long bookId) {
        if (bookId == null) {
            throw new BusinessException(BusinessError.BOOK_NOT_FOUND);
        }
        bookRepository.findById(bookId)
            .orElseThrow(() -> new BusinessException(BusinessError.BOOK_NOT_FOUND));
        return reviewRepository.getReviewByBookId(bookId).stream().map(responseMapper::toReviewResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getPage(Long bookId, Integer rating, String sort, int page, int size) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE
                || (rating != null && (rating < 1 || rating > 5))
                || (!"newest".equals(sort) && !"highest".equals(sort))) {
            throw new BusinessException(BusinessError.VALIDATION_FAILED);
        }
        bookRepository.findById(bookId)
                .filter(candidate -> candidate.getStatus() == BookStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(BusinessError.BOOK_NOT_FOUND));
        return responseMapper.toPageResponse(reviewRepository.findPage(bookId, rating, sort, page, size),
                responseMapper::toReviewResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewSummaryResponse getSummary(Long bookId) {
        bookRepository.findById(bookId)
                .filter(candidate -> candidate.getStatus() == BookStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(BusinessError.BOOK_NOT_FOUND));
        return reviewRepository.getSummary(bookId);
    }

    @Override
    @Transactional(readOnly = true)
    public MyReviewResponse getMyReview(Long bookId, Long customerId) {
        if (customerId == null) throw new BusinessException(BusinessError.UNAUTHORIZED);
        bookRepository.findById(bookId)
                .filter(candidate -> candidate.getStatus() == BookStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(BusinessError.BOOK_NOT_FOUND));
        var review = reviewRepository.findByBookIdAndCustomerId(bookId, customerId);
        return MyReviewResponse.builder().hasReviewed(review.isPresent())
                .review(review.map(responseMapper::toReviewResponse).orElse(null)).build();
    }

}
