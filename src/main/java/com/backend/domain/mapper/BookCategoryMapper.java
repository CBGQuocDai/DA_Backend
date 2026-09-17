package com.backend.domain.mapper;

import com.backend.domain.model.BookCategory;
import com.backend.infrastructure.persistence.entity.JpaBookCategoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {BookMapper.class, CategoryMapper.class})
public interface BookCategoryMapper {

    BookCategory toDomain(JpaBookCategoryEntity entity);

    @Mapping(target = "bookId", source = "book.id")
    @Mapping(target = "categoryId", source = "category.id")
    JpaBookCategoryEntity toEntity(BookCategory domain);
}
