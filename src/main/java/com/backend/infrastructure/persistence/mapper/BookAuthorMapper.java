package com.backend.infrastructure.persistence.mapper;

import com.backend.domain.model.BookAuthor;
import com.backend.infrastructure.persistence.entity.JpaBookAuthorEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {BookMapper.class, AuthorMapper.class})
public interface BookAuthorMapper {

    BookAuthor toDomain(JpaBookAuthorEntity entity);

    // Liên kết với sách được gán tại repository adapter.
    @Mapping(target = "bookId", ignore = true)
    @Mapping(target = "book", ignore = true)
    @Mapping(target = "authorId", source = "author.id")
    JpaBookAuthorEntity toEntity(BookAuthor domain);
}
