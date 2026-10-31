package com.backend.application.service.impl;

import com.backend.application.dto.response.book.BookResponse;
import com.backend.application.mapper.ResponseMapper;
import com.backend.application.adapter.repository.BookRepository;
import com.backend.application.adapter.repository.CustomerBookRepository;
import com.backend.application.service.CustomerBookService;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.CustomerBook;
import com.backend.domain.valueobject.BusinessError;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerBookServiceImpl implements CustomerBookService {

    private final CustomerBookRepository customerBookRepository;
    private final BookRepository bookRepository;
    private final ResponseMapper responseMapper;

    @Override
    @Transactional(readOnly = true)
    public boolean hasPurchasedBook(Long customerId, Long bookId) {
        if (customerId == null || bookId == null) {
            return false;
        }
        return customerBookRepository.existsByCustomerIdAndBookId(customerId, bookId);
    }

    @Override
    @Transactional
    public void assignBookToCustomer(Long customerId, Long bookId) {
        if (customerId == null) {
            throw new BusinessException(BusinessError.UNAUTHORIZED);
        }
        if (bookId == null) {
            throw new BusinessException(BusinessError.BOOK_NOT_FOUND);
        }

        if (customerBookRepository.existsByCustomerIdAndBookId(customerId, bookId)) {
            throw new BusinessException(BusinessError.BOOK_ALREADY_OWNED);
        }

        customerBookRepository.save(CustomerBook.builder()
                .customerId(customerId)
                .bookId(bookId)
                .purchaseAt(Instant.now())
                .isReadComplete(false)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookResponse> getPurchasedBooks(Long customerId) {
        if (customerId == null) {
            throw new BusinessException(BusinessError.UNAUTHORIZED);
        }

        List<Long> bookIds = customerBookRepository.findByCustomerId(customerId).stream()
                .map(CustomerBook::getBookId)
                .distinct()
                .toList();
        if (bookIds.isEmpty()) {
            return List.of();
        }
        return bookRepository.findAllById(bookIds).stream().map(responseMapper::toBookResponse).toList();
    }
}
