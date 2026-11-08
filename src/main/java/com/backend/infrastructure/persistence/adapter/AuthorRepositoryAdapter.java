package com.backend.infrastructure.persistence.adapter;

import com.backend.application.adapter.repository.AuthorRepository;
import com.backend.application.dto.response.PageResponse;
import com.backend.domain.model.Author;
import com.backend.infrastructure.persistence.mapper.AuthorMapper;
import com.backend.infrastructure.persistence.repository.JpaAuthorRepository;
import com.backend.infrastructure.persistence.repository.JpaBookAuthorRepository;
import com.backend.infrastructure.persistence.util.LikePatterns;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AuthorRepositoryAdapter implements AuthorRepository {

    private final JpaAuthorRepository jpaAuthorRepository;
    private final JpaBookAuthorRepository jpaBookAuthorRepository;
    private final AuthorMapper authorMapper;

    private static final String ACCENTED = "àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ";
    private static final String PLAIN = normalize(ACCENTED);

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Author> searchByName(String keyword, int page, int size) {
        String pattern = "%" + normalize(keyword == null ? "" : keyword)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return LikePatterns.toPageResponse(
                jpaAuthorRepository.searchByName(pattern, ACCENTED, PLAIN, PageRequest.of(page, size)),
                authorMapper::toDomain);
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT).replace('đ', 'd').trim();
    }

    @Override
    public List<Author> findAll() {
        return jpaAuthorRepository.findAll().stream()
                .map(authorMapper::toDomain)
                .toList();
    }

    @Override
    public PageResponse<Author> search(String keyword, int page, int size) {
        return LikePatterns.toPageResponse(
                jpaAuthorRepository.search(LikePatterns.contains(keyword), PageRequest.of(page, size)),
                authorMapper::toDomain);
    }

    @Override
    public Optional<Author> findById(Long id) {
        return jpaAuthorRepository.findById(id).map(authorMapper::toDomain);
    }

    @Override
    public Author save(Author author) {
        return authorMapper.toDomain(jpaAuthorRepository.save(authorMapper.toEntity(author)));
    }

    @Override
    public void deleteById(Long id) {
        jpaAuthorRepository.deleteById(id);
    }

    @Override
    public boolean isUsedByAnyBook(Long id) {
        return jpaBookAuthorRepository.existsByAuthorId(id);
    }
}
