package com.backend.domain.mapper;

import com.backend.domain.dto.request.book.BookCreateRequest;
import com.backend.domain.model.Book;
import com.backend.infrastructure.persistence.entity.JpaBookEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {VoiceMapper.class})
public interface BookMapper {

    Book toDomain(JpaBookEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isPublish", ignore = true)
    @Mapping(target = "coverImage", ignore = true)
    @Mapping(target = "bookCategories", ignore = true)
    @Mapping(target = "bookAuthors", ignore = true)
    @Mapping(target = "voice", source = "voice")
    Book toDomain(BookCreateRequest request);

    JpaBookEntity toEntity(Book domain);
}

