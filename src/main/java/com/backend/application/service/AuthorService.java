package com.backend.application.service;

import com.backend.application.dto.response.author.AuthorResponse;
import com.backend.application.dto.response.author.CustomerAuthorResponse;
import com.backend.application.dto.response.PageResponse;

import java.util.List;


public interface AuthorService {
    List<AuthorResponse> getAuthors();
    PageResponse<CustomerAuthorResponse> getCustomerAuthors(String keyword, String sort, int page, int size);
    CustomerAuthorResponse getCustomerAuthorById(Long authorId);
}
