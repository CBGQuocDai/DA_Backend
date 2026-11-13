package com.backend.application.service.impl;

import com.backend.application.adapter.repository.AuthorRepository;
import com.backend.application.adapter.storage.StorageService;
import com.backend.application.dto.request.author.AuthorRequest;
import com.backend.application.dto.response.PageResponse;
import com.backend.application.dto.response.author.AuthorResponse;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.valueobject.BusinessError;
import com.backend.application.mapper.ResponseMapper;
import com.backend.application.service.AuthorService;
import com.backend.application.service.FileManagementService;
import com.backend.domain.model.Author;
import com.backend.domain.valueobject.UploadType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorServiceImpl implements AuthorService {

    private final AuthorRepository authorRepository;
    private final FileManagementService fileManagementService;
    private final StorageService storageService;
    private final ResponseMapper responseMapper;

    @Override
    public List<AuthorResponse> getAuthors() {
        return authorRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(Author::getFullName, String.CASE_INSENSITIVE_ORDER))
                .map(responseMapper::toAuthorResponse).toList();
    }

    @Override
    public PageResponse<AuthorResponse> searchAuthors(String keyword, int page, int size) {
        return responseMapper.toPageResponse(
                authorRepository.searchByName(keyword, page, size), responseMapper::toAuthorResponse);
    }

    @Override
    public AuthorResponse getAuthor(Long authorId) {
        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new BusinessException(BusinessError.AUTHOR_NOT_FOUND));
        return responseMapper.toAuthorResponse(author);
    }

    @Override
    @Transactional
    public AuthorResponse createAuthor(AuthorRequest req) {
        String avatar = blankToNull(req.getAvatar());
        if (avatar != null) {
            fileManagementService.verifyUpload(avatar, UploadType.IMAGE);
        }
        Author author = new Author();
        applyRequest(author, req, avatar == null ? null : UploadType.IMAGE.permanentPath(avatar));
        Author saved = authorRepository.save(author);
        if (avatar != null) {
            fileManagementService.moveToPermanent(avatar, UploadType.IMAGE);
        }
        return responseMapper.toAuthorResponse(saved);
    }

    @Override
    @Transactional
    public AuthorResponse updateAuthor(Long authorId, AuthorRequest req) {
        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new BusinessException(BusinessError.AUTHOR_NOT_FOUND));
        String oldAvatar = author.getAvatar();
        String requested = blankToNull(req.getAvatar());
        boolean newAvatar = requested != null && !requested.equals(oldAvatar);

        if (newAvatar) {
            fileManagementService.verifyUpload(requested, UploadType.IMAGE);
        }
        String avatar = newAvatar ? UploadType.IMAGE.permanentPath(requested) : requested;
        applyRequest(author, req, avatar);
        Author saved = authorRepository.save(author);
        if (newAvatar) {
            fileManagementService.moveToPermanent(requested, UploadType.IMAGE);
        }
        if (oldAvatar != null && !Objects.equals(oldAvatar, avatar)) {
            deleteQuietly(oldAvatar);
        }
        return responseMapper.toAuthorResponse(saved);
    }

    @Override
    @Transactional
    public void deleteAuthor(Long authorId) {
        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new BusinessException(BusinessError.AUTHOR_NOT_FOUND));
        if (authorRepository.isUsedByAnyBook(authorId)) {
            throw new BusinessException(BusinessError.AUTHOR_IN_USE);
        }
        authorRepository.deleteById(authorId);
        if (author.getAvatar() != null) {
            deleteQuietly(author.getAvatar());
        }
    }

    private void applyRequest(Author author, AuthorRequest req, String avatar) {
        author.setFullName(req.getFullName().trim());
        author.setBio(blankToNull(req.getBio()));
        author.setAvatar(avatar);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void deleteQuietly(String path) {
        try {
            storageService.delete(path);
        } catch (IOException e) {
            log.warn("Failed to delete author avatar {}", path, e);
        }
    }
}
