package com.backend.domain.mapper;

import com.backend.domain.model.Author;
import com.backend.infrastructure.persistence.entity.JpaAuthorEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthorMapper {

    Author toDomain(JpaAuthorEntity entity);

    JpaAuthorEntity toEntity(Author domain);
}
