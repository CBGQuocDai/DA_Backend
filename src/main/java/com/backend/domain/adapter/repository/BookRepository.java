package com.backend.domain.adapter.repository;


import com.backend.domain.dto.response.PageResponse;
import com.backend.domain.model.Book;

import java.util.Optional;

public interface BookRepository {
    Book save(Book book);

    Optional<Book> findById(Long id);

    PageResponse<Book> findAll(int page, int size);
}
