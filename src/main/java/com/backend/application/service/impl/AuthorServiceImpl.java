package com.backend.application.service.impl;

import com.backend.application.service.AuthorService;
import com.backend.application.adapter.repository.AuthorRepository;
import com.backend.application.dto.response.author.AuthorResponse;
import com.backend.application.dto.response.author.CustomerAuthorResponse;
import com.backend.application.dto.response.PageResponse;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.AuthorSummary;
import com.backend.domain.valueobject.BusinessError;
import com.backend.application.mapper.ResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthorServiceImpl implements AuthorService {

    private final AuthorRepository authorRepository;
    private final ResponseMapper responseMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerAuthorResponse> getCustomerAuthors(String keyword, String sort,
        int page, int size) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE
            || keyword != null && keyword.length() > 200
            || sort == null || !Set.of("name_asc", "book_count_desc").contains(sort)) {
            throw new BusinessException(BusinessError.VALIDATION_FAILED);
        }
        return responseMapper.toPageResponse(
            authorRepository.findCustomerPage(keyword, sort, page, size),
            this::toCustomerAuthorResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerAuthorResponse getCustomerAuthorById(Long authorId) {
        if (authorId == null || authorId <= 0) {
            throw new BusinessException(BusinessError.VALIDATION_FAILED);
        }
        AuthorSummary author = authorRepository.findCustomerById(authorId)
            .orElseThrow(() -> new BusinessException(BusinessError.NOT_FOUND));
        return toCustomerAuthorResponse(author);
    }

    private CustomerAuthorResponse toCustomerAuthorResponse(AuthorSummary author) {
        return CustomerAuthorResponse.builder()
            .id(author.getId()).fullName(author.getFullName()).avatar(author.getAvatar())
            .bio(author.getBio()).specialty(author.getSpecialty())
            .bookCount(author.getBookCount())
            .build();
    }

    @Override
    public List<AuthorResponse> getAuthors() {

        return authorRepository.findAll().stream().map(responseMapper::toAuthorResponse).toList();
    }
}
