package com.backend.application.service.impl;

import com.backend.application.service.CustomerBookService;
import com.backend.domain.adapter.repository.BookRepository;
import com.backend.domain.adapter.repository.CustomerBookRepository;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Book;
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

        CustomerBook customerBook = CustomerBook.builder()
                .customerId(customerId)
                .bookId(bookId)
                .purchaseAt(Instant.now())
                .isReadComplete(false)
                .build();

        customerBookRepository.save(customerBook);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Book> getPurchasedBooks(Long customerId) {
        if (customerId == null) {
            throw new BusinessException(BusinessError.UNAUTHORIZED);
        }

        List<CustomerBook> customerBooks = customerBookRepository.findByCustomerId(customerId);
        if (customerBooks == null || customerBooks.isEmpty()) {
            return List.of();
        }

        List<Long> bookIds = customerBooks.stream()
                .map(CustomerBook::getBookId)
                .distinct()
                .toList();

        return bookRepository.findAllById(bookIds);
    }
}

