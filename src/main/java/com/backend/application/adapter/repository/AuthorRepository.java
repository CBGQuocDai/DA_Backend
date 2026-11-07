package com.backend.application.adapter.repository;

import com.backend.application.dto.response.PageResponse;
import com.backend.domain.model.Author;
import com.backend.domain.model.AuthorSummary;

import java.util.List;
import java.util.Optional;

public interface AuthorRepository {
    List<Author> findAll();
    PageResponse<AuthorSummary> findCustomerPage(String keyword, String sort, int page, int size);
    Optional<AuthorSummary> findCustomerById(Long authorId);

    /** Newest first; a blank keyword matches every author. */
    PageResponse<Author> search(String keyword, int page, int size);

    Optional<Author> findById(Long id);

    Author save(Author author);

    void deleteById(Long id);

    boolean isUsedByAnyBook(Long id);
}
