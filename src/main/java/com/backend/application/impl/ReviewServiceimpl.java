package com.backend.application.impl;

import com.backend.application.ReviewService;
import com.backend.domain.adapter.repository.BookRepository;
import com.backend.domain.adapter.repository.CustomerRepository;
import com.backend.domain.adapter.repository.ReviewRepository;
import com.backend.domain.dto.request.review.ReviewCreateRequest;

import com.backend.domain.dto.request.user.customer.CustomerChangePasswordRequest;
import com.backend.domain.dto.response.customer.CustomerProfileResponse;
import com.backend.domain.dto.response.review.ReviewCreateResponse;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Book;
import com.backend.domain.model.Customer;
import com.backend.domain.model.Review;
import com.backend.domain.valueobject.BusinessError;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewServiceimpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public ReviewCreateResponse save(Long userId, ReviewCreateRequest request) {
        if (userId == null) {
            throw new BusinessException(BusinessError.USER_NOT_FOUND);
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
    public List<Review> getReviewByBookId(Long bookId) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BusinessException(BusinessError.BOOK_NOT_FOUND));
        return reviewRepository.getReviewByBookId(bookId);
    }

}
