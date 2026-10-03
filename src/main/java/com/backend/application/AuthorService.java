package com.backend.application;

import com.backend.domain.model.Author;

import java.util.List;

public interface AuthorService {
    List<Author> getAuthors();
}
