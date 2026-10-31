package com.backend.application.service.impl;

import com.backend.application.service.AuthorService;
import com.backend.application.adapter.repository.AuthorRepository;
import com.backend.application.dto.response.author.AuthorResponse;
import com.backend.application.mapper.ResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthorServiceImpl implements AuthorService {

    private final AuthorRepository authorRepository;
    private final ResponseMapper responseMapper;

    @Override
    public List<AuthorResponse> getAuthors() {

        return authorRepository.findAll().stream().map(responseMapper::toAuthorResponse).toList();
    }
}
