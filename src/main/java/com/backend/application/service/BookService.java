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

    List<CustomerChapterResponse> getCustomerChapters(Long bookId);

    PageResponse<BookResponse> getCustomerBooks(String bookName, Long categoryId, Long authorId, int page, int size);

    /** Lấy danh sách sách gợi ý mới nhất */
    List<BookResponse> getRecommendations(int limit);

    /** Lấy gợi ý sách cá nhân hóa theo thể loại cho khách hàng (kèm bù sách mới). */
    List<BookResponse> getCustomerRecommendations(Long customerId, int limit);

    /** Lấy danh sách sách tương tự (cùng thể loại hoặc cùng tác giả) với sách đang xem. */
    List<BookResponse> getSimilarBooks(Long bookId, int limit);

    /** Admin dashboard: the most purchased books in the period, each with the period before it. */
    List<BookResponse> getBestsellers(BestsellerPeriod period);
}
