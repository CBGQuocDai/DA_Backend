package com.backend.application.service.impl;

import com.backend.application.service.ReviewService;
import com.backend.domain.adapter.repository.BookRepository;
import com.backend.domain.adapter.repository.CustomerRepository;
import com.backend.domain.adapter.repository.ReviewRepository;
import com.backend.domain.dto.request.review.ReviewCreateRequest;
import com.backend.domain.dto.response.review.ReviewCreateResponse;
import com.backend.domain.dto.response.review.ReviewResponse;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.mapper.ReviewMapper;
import com.backend.domain.model.Book;
import com.backend.domain.model.Customer;
import com.backend.domain.model.Review;
import com.backend.domain.valueobject.BusinessError;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final CustomerRepository customerRepository;
    private final ReviewMapper reviewMapper;

    @Override
    @Transactional
    public ReviewCreateResponse save(Long userId, ReviewCreateRequest request) {
        if (userId == null) {
            throw new BusinessException(BusinessError.USER_NOT_FOUND);
        }
        if (request.getBookId() == null) {
            throw new BusinessException(BusinessError.BOOK_NOT_FOUND);
        }

        Customer customer = customerRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(BusinessError.USER_NOT_FOUND));
        Book book = bookRepository.findById(request.getBookId())
            .orElseThrow(() -> new BusinessException(BusinessError.BOOK_NOT_FOUND));

        Review review = Review.builder()
            .rate(request.getRate())
            .content(request.getContent())
            .customer(customer)
            .book(book)
            .build();
        Review saved = reviewRepository.save(review);

        return ReviewCreateResponse.builder()
            .id(saved.getId())
            .rate(saved.getRate())
            .content(saved.getContent())
            .createAt(saved.getCreateAt())
            .customerId(userId)
            .bookId(request.getBookId())
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewByBookId(Long bookId) {
        if (bookId == null) {
            throw new BusinessException(BusinessError.BOOK_NOT_FOUND);
        }
        bookRepository.findById(bookId)
            .orElseThrow(() -> new BusinessException(BusinessError.BOOK_NOT_FOUND));

        List<Review> reviews = reviewRepository.getReviewByBookId(bookId);
        return reviews.stream()
                .map(reviewMapper::toResponse)
                .toList();
    }
}
