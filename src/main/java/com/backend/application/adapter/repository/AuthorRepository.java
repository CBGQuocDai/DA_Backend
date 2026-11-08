package com.backend.application.adapter.repository;

import com.backend.application.dto.response.PageResponse;
import com.backend.domain.model.Author;

import java.util.List;
import java.util.Optional;

public interface AuthorRepository {
    List<Author> findAll();

    /** Newest first; a blank keyword matches every author. */
    PageResponse<Author> search(String keyword, int page, int size);

    /** Accent-insensitive name match, sorted by name; a blank keyword matches every author. */
    PageResponse<Author> searchByName(String keyword, int page, int size);

    Optional<Author> findById(Long id);

    Author save(Author author);

    void deleteById(Long id);

    boolean isUsedByAnyBook(Long id);
}
