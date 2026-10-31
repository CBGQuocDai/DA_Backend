package com.backend.application.service;

import com.backend.application.dto.response.author.AuthorResponse;

import java.util.List;


public interface AuthorService {
    List<AuthorResponse> getAuthors();
}
