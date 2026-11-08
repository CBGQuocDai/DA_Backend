package com.backend.application.service;

import com.backend.application.dto.request.author.AuthorRequest;
import com.backend.application.dto.response.PageResponse;
import com.backend.application.dto.response.author.AuthorResponse;

import java.util.List;


public interface AuthorService {
    /** Every author, for pickers such as the book form. */
    List<AuthorResponse> getAuthors();

    /** Admin list: filtered by name, newest first. */
    PageResponse<AuthorResponse> searchAuthors(String keyword, int page, int size);

    AuthorResponse getAuthor(Long authorId);

    AuthorResponse createAuthor(AuthorRequest request);

    AuthorResponse updateAuthor(Long authorId, AuthorRequest request);

    /** Refused while any book still lists the author. */
    void deleteAuthor(Long authorId);
    /** Public list: accent-insensitive name filter, sorted by name. */
    PageResponse<AuthorResponse> getCustomerAuthors(String keyword, int page, int size);
}
