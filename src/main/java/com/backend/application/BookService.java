package com.backend.application;


import com.backend.domain.dto.request.book.BookCreateRequest;
import com.backend.domain.dto.response.PageResponse;
import com.backend.domain.model.Book;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface BookService {
//  chức năng của admin
    PageResponse<Book> getBooks(int page, int size);
    Book createBook(BookCreateRequest book, MultipartFile pdfFile, MultipartFile bookCover) throws IOException;
    Book updateBook(Book book);
    void deleteBook(Book book);
    void publishBook(Long bookId);
    void unpublishBook(Long bookId);
// chức năng của client


}
