package com.backend.application.adapter.repository;

import com.backend.application.dto.response.PageResponse;
import com.backend.domain.model.Author;

import java.util.List;

public interface AuthorRepository {
    List<Author> findAll();
}
