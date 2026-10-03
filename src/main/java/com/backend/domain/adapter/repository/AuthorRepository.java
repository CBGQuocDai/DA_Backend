package com.backend.domain.adapter.repository;

import com.backend.domain.model.Author;

import java.util.List;

public interface AuthorRepository {
    List<Author> findAll();
}
