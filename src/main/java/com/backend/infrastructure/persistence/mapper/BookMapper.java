package com.backend.infrastructure.persistence.mapper;

import com.backend.domain.model.Book;
import com.backend.infrastructure.persistence.entity.JpaBookEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {VoiceMapper.class})
public interface BookMapper {

    Book toDomain(JpaBookEntity entity);

    JpaBookEntity toEntity(Book domain);
}

