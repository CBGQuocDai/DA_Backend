package com.backend.application.impl;

import com.backend.application.BookFavoriteService;
import com.backend.domain.adapter.repository.BookFavoriteRepository;
import com.backend.domain.adapter.repository.BookRepository;
import com.backend.domain.adapter.repository.CustomerRepository;
import com.backend.domain.dto.response.BookFavoriteResponse;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Book;
import com.backend.domain.model.BookFavorite;
import com.backend.domain.model.Customer;
import com.backend.domain.valueobject.BusinessError;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookFavoriteServiceImpl implements BookFavoriteService {

    private final BookFavoriteRepository bookFavoriteRepository;
    private final CustomerRepository customerRepository;
    private final BookRepository bookRepository;

    @Override
    public BookFavoriteResponse addFavorite(Long userId, Long bookId) {
        if (bookFavoriteRepository.existsByCustomerIdAndBookId(userId, bookId)) {
            throw new BusinessException(BusinessError.BOOK_ALREADY_FAVORITED);
        }
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
            .build();
    }

    @Override
    public Void removeFavorite(Long userId, Long bookId) {
        return null;
    }

    @Override
    public List<Book> list(Long userId) {
        return List.of();
    }
}
