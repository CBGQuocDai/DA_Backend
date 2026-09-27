package com.backend.domain.adapter.repository;


import com.backend.domain.dto.response.PageResponse;
import com.backend.domain.model.Book;

import java.util.Optional;

public interface BookRepository {
    Book insert(Book book);

    void update(Book book);

    Optional<Book> findById(Long id);

    PageResponse<Book> findAll(int page, int size);

    void delete(Book book);

    void deleteById(Long id);
}
