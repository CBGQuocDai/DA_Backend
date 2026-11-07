package com.backend.application.service;

import com.backend.application.dto.request.author.AuthorRequest;
import com.backend.application.dto.response.PageResponse;
import com.backend.application.dto.response.author.AuthorResponse;
import com.backend.application.dto.response.author.CustomerAuthorResponse;
import com.backend.application.dto.response.PageResponse;

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
    PageResponse<CustomerAuthorResponse> getCustomerAuthors(String keyword, String sort, int page, int size);
    CustomerAuthorResponse getCustomerAuthorById(Long authorId);
}
