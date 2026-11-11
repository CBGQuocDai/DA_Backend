package com.backend.application.service;

import com.backend.application.dto.request.book.BookRequest;
import com.backend.application.dto.response.PageResponse;
import com.backend.application.dto.response.book.BookResponse;
import com.backend.application.dto.response.book.CustomerBookOverviewResponse;
import com.backend.application.dto.response.chapter.CustomerChapterResponse;
import com.backend.domain.valueobject.BestsellerPeriod;
import com.backend.domain.valueobject.BookStatus;

import java.util.List;

public interface BookService {

    //  chức năng của admin
    PageResponse<BookResponse> getBooks(String bookName, Long categoryId, Long authorId,
        BookStatus status, int page, int size);

    BookResponse getBookById(Long bookId);

    BookResponse createBook(BookRequest request);

    BookResponse updateBook(Long bookId, BookRequest request);

    void deleteBook(Long bookId);

    void publishBook(Long bookId);

    void unpublishBook(Long bookId);

// chức năng customer

    CustomerBookOverviewResponse getCustomerBookOverview(Long bookId);

    PageResponse<CustomerChapterResponse> getCustomerChapters(Long bookId, int page, int size);

    /** Admin dashboard: the most purchased books in the period, each with the period before it. */
    List<BookResponse> getBestsellers(BestsellerPeriod period);
}
