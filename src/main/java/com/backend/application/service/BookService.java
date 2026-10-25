package com.backend.application.service;

import com.backend.domain.dto.response.PageResponse;
import com.backend.domain.model.Book;
import com.backend.domain.model.BookStat;
import com.backend.domain.valueobject.BookStatus;
import org.springframework.web.multipart.MultipartFile;


public interface BookService {
//  chức năng của admin
    PageResponse<BookStat> getBooks(String bookName, Long categoryId, Long authorId, BookStatus status, int page, int size);
    Book getBookById(Long bookId);
    Book createBook(String book, MultipartFile pdfFile, MultipartFile bookCover);
    Book updateBook(Long bookId, String book, MultipartFile bookCover, MultipartFile contentFile);
    void deleteBook(Long bookId);
    void publishBook(Long bookId);
    void unpublishBook(Long bookId);
// chức năng của client
}
