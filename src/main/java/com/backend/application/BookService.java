package com.backend.application;

import com.backend.domain.dto.response.PageResponse;
import com.backend.domain.model.Book;
import com.backend.domain.model.BookStat;
import org.springframework.web.multipart.MultipartFile;


public interface BookService {
//  chức năng của admin
    PageResponse<BookStat> getBooks(int page, int size);
    Book getBookById(Long bookId);
    Book createBook(String book, MultipartFile pdfFile, MultipartFile bookCover);
    Book updateBook(Long bookId, String book, MultipartFile bookCover);
    void deleteBook(Long bookId);
    void publishBook(Long bookId);
    void unpublishBook(Long bookId);
// chức năng của client
}
