package com.backend.infrastructure.persistence.adapter;

import com.backend.application.adapter.repository.AuthorRepository;
import com.backend.application.dto.response.PageResponse;
import com.backend.infrastructure.persistence.mapper.AuthorMapper;
import com.backend.infrastructure.persistence.mapper.AuthorSummaryMapper;
import com.backend.domain.model.Author;
import com.backend.domain.model.AuthorSummary;
import com.backend.infrastructure.persistence.projection.AuthorListProjection;
import com.backend.infrastructure.persistence.projection.AuthorSpecialtyProjection;
import com.backend.infrastructure.persistence.repository.JpaAuthorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AuthorRepositoryAdapter implements AuthorRepository {

    private final JpaAuthorRepository jpaAuthorRepository;
    private final AuthorMapper authorMapper;

    private static final String ACCENTED = "àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ";
    private static final String PLAIN = normalize(ACCENTED);

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuthorSummary> findCustomerPage(String keyword, String sort, int page, int size) {
        String pattern = "%" + normalize(keyword == null ? "" : keyword)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        Page<AuthorListProjection> projectionPage = jpaAuthorRepository.findCustomerAuthorProjections(
                pattern, ACCENTED, PLAIN, sort, PageRequest.of(page, size));

        List<Long> authorIds = projectionPage.getContent().stream()
                .map(AuthorListProjection::getId)
                .toList();
        Map<Long, List<String>> specialties = loadSpecialties(authorIds);
        List<AuthorSummary> authors = projectionPage.getContent().stream()
                .map(projection -> AuthorSummaryMapper.toAuthorSummary(
                        projection, specialties.getOrDefault(projection.getId(), List.of())))
                .toList();

        return PageResponse.<AuthorSummary>builder()
                .content(authors)
                .total((int) projectionPage.getTotalElements())
                .totalPage(projectionPage.getTotalPages())
                .page(page)
                .pageSize(size)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthorSummary> findCustomerById(Long authorId) {
        return jpaAuthorRepository.findCustomerAuthorProjectionById(authorId)
                .map(projection -> {
                    List<String> genres = jpaAuthorRepository.findSpecialtiesByAuthorIdIn(List.of(authorId)).stream()
                            .map(AuthorSpecialtyProjection::getCategoryName)
                            .toList();
                    return AuthorSummaryMapper.toAuthorSummary(projection, genres);
                });
    }

    private Map<Long, List<String>> loadSpecialties(List<Long> authorIds) {
        if (authorIds.isEmpty()) return Map.of();
        List<AuthorSpecialtyProjection> rows = jpaAuthorRepository.findSpecialtiesByAuthorIdIn(authorIds);
        Map<Long, List<String>> result = new LinkedHashMap<>();
        for (AuthorSpecialtyProjection row : rows) {
            result.computeIfAbsent(row.getAuthorId(), ignored -> new ArrayList<>()).add(row.getCategoryName());
        }
        return result;
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
}
