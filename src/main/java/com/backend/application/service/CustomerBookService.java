package com.backend.application.service;

public interface CustomerBookService {

    boolean hasPurchasedBook(Long customerId, Long bookId);

    void assignBookToCustomer(Long customerId, Long bookId);
}
