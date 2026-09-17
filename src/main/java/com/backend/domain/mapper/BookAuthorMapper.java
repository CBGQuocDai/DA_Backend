package com.backend.domain.mapper;

import com.backend.domain.model.BookAuthor;
import com.backend.infrastructure.persistence.entity.JpaBookAuthorEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {BookMapper.class, AuthorMapper.class})
public interface BookAuthorMapper {

    BookAuthor toDomain(JpaBookAuthorEntity entity);

    @Mapping(target = "bookId", source = "book.id")
    @Mapping(target = "authorId", source = "author.id")
    JpaBookAuthorEntity toEntity(BookAuthor domain);
}
