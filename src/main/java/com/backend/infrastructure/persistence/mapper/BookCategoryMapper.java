package com.backend.infrastructure.persistence.mapper;

import com.backend.domain.model.BookCategory;
import com.backend.infrastructure.persistence.entity.JpaBookCategoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = {BookMapper.class, CategoryMapper.class})
public interface BookCategoryMapper {

    BookCategory toDomain(JpaBookCategoryEntity entity);

    /** Dùng khi nạp theo lô: bỏ qua {@code book} (LAZY) để không phát sinh 1 query mỗi dòng. */
    @Named("toDomainWithoutBook")
    @Mapping(target = "book", ignore = true)
    BookCategory toDomainWithoutBook(JpaBookCategoryEntity entity);

    @Mapping(target = "bookId", source = "book.id")
    @Mapping(target = "categoryId", source = "category.id")
    JpaBookCategoryEntity toEntity(BookCategory domain);
}
