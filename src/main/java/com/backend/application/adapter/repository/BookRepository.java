package com.backend.application.adapter.repository;


import com.backend.application.dto.response.PageResponse;
import com.backend.domain.model.Book;
import com.backend.domain.model.BookStat;
import com.backend.domain.valueobject.BookStatus;

import java.util.List;
import java.util.Optional;

public interface BookRepository {

    Book insert(Book book);

    void update(Book book);

    Optional<Book> findById(Long id);
    /** Only the book's own fields, without its authors, categories and chapters. */
    Optional<Book> findBasicById(Long id);

    PageResponse<BookStat> findAllWithStats(String bookName, Long categoryId, Long authorId,
                                            BookStatus status, int page, int size);

    PageResponse<BookStat> findByCategoryId(Long categoryId, int page, int size);
    void deleteById(Long id);

    List<Book> findAllById(List<Long> bookIds);
}
