package com.backend.domain.mapper;

import com.backend.domain.model.Book;
import com.backend.infrastructure.persistence.entity.JpaBookEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {VocieMapper.class})
public interface BookMapper {

    Book toDomain(JpaBookEntity entity);

    @Mapping(target = "voiceId", source = "voice.id")
    JpaBookEntity toEntity(Book domain);
}

