package com.backend.application.service;

import com.backend.domain.model.Book;
import java.util.List;

public interface CustomerBookService {

    boolean hasPurchasedBook(Long customerId, Long bookId);

    void assignBookToCustomer(Long customerId, Long bookId);

    List<Book> getPurchasedBooks(Long customerId);
}
