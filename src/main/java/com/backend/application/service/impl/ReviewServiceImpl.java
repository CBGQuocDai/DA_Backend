package com.backend.application.service.impl;

import com.backend.application.dto.request.review.ReviewCreateRequest;
import com.backend.application.dto.response.review.ReviewCreateResponse;
import com.backend.application.dto.response.review.ReviewResponse;
import com.backend.application.mapper.ResponseMapper;
import com.backend.application.adapter.repository.BookRepository;
import com.backend.application.adapter.repository.CustomerRepository;
import com.backend.application.adapter.repository.ReviewRepository;
import com.backend.application.service.ReviewService;
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
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final CustomerRepository customerRepository;
    private final ResponseMapper responseMapper;

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
        return reviewRepository.getReviewByBookId(bookId).stream().map(responseMapper::toReviewResponse).toList();
    }
}
