package com.backend.infrastructure.persistence.mapper;

import com.backend.domain.model.AuthorSummary;
import com.backend.infrastructure.persistence.projection.AuthorListProjection;

import java.util.List;

public final class AuthorSummaryMapper {

    private AuthorSummaryMapper() {}

    public static AuthorSummary toAuthorSummary(AuthorListProjection projection, List<String> genres) {
        return AuthorSummary.builder()
                .id(projection.getId())
                .fullName(projection.getFullName())
                .avatar(projection.getAvatar())
                .bio(projection.getBio())
                .bookCount(projection.getBookCount())
                .specialty(genres.isEmpty() ? null : String.join(" & ", genres.stream().limit(2).toList()))
                .build();
    }
}
