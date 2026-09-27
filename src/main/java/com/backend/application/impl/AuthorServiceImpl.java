package com.backend.application.impl;

import com.backend.application.AuthorService;
import com.backend.domain.adapter.repository.AuthorRepository;
import com.backend.domain.model.Author;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthorServiceImpl implements AuthorService {

    private final AuthorRepository authorRepository;

    @Override
    public List<Author> getAuthors() {
        return authorRepository.findAll();
    }
}
