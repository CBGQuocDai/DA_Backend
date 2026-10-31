package com.backend.application.service;

import com.backend.application.dto.request.book.BookRequest;
import com.backend.application.dto.response.PageResponse;
import com.backend.application.dto.response.book.BookResponse;
import com.backend.domain.valueobject.BookStatus;


public interface BookService {
//  chức năng của admin
    PageResponse<BookResponse> getBooks(String bookName, Long categoryId, Long authorId, BookStatus status, int page, int size);
    BookResponse getBookById(Long bookId);
    BookResponse createBook(BookRequest request);
    BookResponse updateBook(Long bookId, BookRequest request);
    void deleteBook(Long bookId);
    void publishBook(Long bookId);
    void unpublishBook(Long bookId);
// chức năng của client
    PageResponse<BookResponse> getBooksByCategory(Long categoryId, int page, int size);
}
