package com.backend.infrastructure.persistence.adapter;

import com.backend.domain.adapter.repository.AuthorRepository;
import com.backend.domain.mapper.AuthorMapper;
import com.backend.domain.model.Author;
import com.backend.infrastructure.persistence.repository.JpaAuthorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AuthorRepositoryAdapter implements AuthorRepository {

    private final JpaAuthorRepository jpaAuthorRepository;
    private final AuthorMapper authorMapper;

    @Override
    public List<Author> findAll() {
        return jpaAuthorRepository.findAll().stream()
                .map(authorMapper::toDomain)
                .toList();
    }
}
