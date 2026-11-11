package com.backend.application.service;

import com.backend.application.dto.request.author.AuthorRequest;
import com.backend.application.dto.response.PageResponse;
import com.backend.application.dto.response.author.AuthorResponse;

import java.util.List;


public interface AuthorService {

    List<AuthorResponse> getAuthors();

    PageResponse<AuthorResponse> searchAuthors(String keyword, int page, int size);

    AuthorResponse getAuthor(Long authorId);

    AuthorResponse createAuthor(AuthorRequest request);

    AuthorResponse updateAuthor(Long authorId, AuthorRequest request);

    void deleteAuthor(Long authorId);

}
