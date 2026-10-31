package com.backend.application.service.impl;

import com.backend.application.service.BookFavoriteService;
import com.backend.application.adapter.repository.BookFavoriteRepository;
import com.backend.application.adapter.repository.BookRepository;
import com.backend.application.adapter.repository.CustomerRepository;
import com.backend.application.dto.response.BookFavoriteResponse;
import com.backend.application.dto.response.book.BookResponse;
import com.backend.application.mapper.ResponseMapper;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Book;
import com.backend.domain.model.BookFavorite;
import com.backend.domain.model.Customer;
import com.backend.domain.valueobject.BusinessError;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookFavoriteServiceImpl implements BookFavoriteService {

    private final BookFavoriteRepository bookFavoriteRepository;
    private final CustomerRepository customerRepository;
    private final BookRepository bookRepository;
    private final ResponseMapper responseMapper;

    @Override
    @Transactional
    public BookFavoriteResponse toggleFavorite(Long userId, Long bookId) {
        if (userId == null) {
            throw new BusinessException(BusinessError.USER_NOT_FOUND);
        }
        if (bookId == null) {
            throw new BusinessException(BusinessError.BOOK_NOT_FOUND);
        }

        // Nếu đã yêu thích: Xóa ngay lập tức mà không cần query nặng bảng book
        if (bookFavoriteRepository.existsByCustomerIdAndBookId(userId, bookId)) {
            bookFavoriteRepository.deleteByCustomerIdAndBookId(userId, bookId);
            return BookFavoriteResponse.builder()
                .customerId(userId)
                .bookId(bookId)
                .isFavorite(false)
                .build();
        }

        // Nếu chưa yêu thích: Query và thêm mới
        Customer customer = customerRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(BusinessError.USER_NOT_FOUND));
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BusinessException(BusinessError.BOOK_NOT_FOUND));

        BookFavorite bookFavorite = BookFavorite.builder()
            .customer(customer)
            .book(book)
            .build();
        BookFavorite saved = bookFavoriteRepository.save(bookFavorite);
        return BookFavoriteResponse.builder()
            .id(saved.getId())
            .customerId(userId)
            .bookId(bookId)
            .isFavorite(true)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFavorite(Long userId, Long bookId) {
        if (userId == null || bookId == null) {
            return false;
        }
        return bookFavoriteRepository.existsByCustomerIdAndBookId(userId, bookId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookResponse> list(Long userId) {
        if (userId == null) {
            throw new BusinessException(BusinessError.USER_NOT_FOUND);
        }

        customerRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(BusinessError.USER_NOT_FOUND));

        List<Long> bookIds = bookFavoriteRepository.findBookIdsByCustomerId(userId);
        if (bookIds == null || bookIds.isEmpty()) {
            return List.of();
        }
        return bookRepository.findAllById(bookIds).stream().map(responseMapper::toBookResponse).toList();
    }
}
