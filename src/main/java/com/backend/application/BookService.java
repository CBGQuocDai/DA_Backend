package com.backend.application;


import com.backend.domain.dto.response.PageResponse;
import com.backend.domain.model.Book;

public interface BookService {
//  chức năng của admin
    PageResponse<Book> getBooks();
    Book createBook(Book book);
    Book updateBook(Book book);
    void deleteBook(Book book);
    void publishBook(Long bookId);
    void unpublishBook(Long bookId);
// chức năng của client


}
