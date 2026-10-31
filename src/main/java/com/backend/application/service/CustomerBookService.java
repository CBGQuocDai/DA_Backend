package com.backend.application.service;

import com.backend.application.dto.response.book.BookResponse;
import java.util.List;

public interface CustomerBookService {

    boolean hasPurchasedBook(Long customerId, Long bookId);

    void assignBookToCustomer(Long customerId, Long bookId);

    List<BookResponse> getPurchasedBooks(Long customerId);
}
