package com.backend.domain.mapper;

import com.backend.domain.model.BookFavorite;
import com.backend.infrastructure.persistence.entity.JpaBookFavoriteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {BookMapper.class, CustomerMapper.class})
public interface BookFavoriteMapper {

    @Mapping(target = "customer", ignore = true)
    BookFavorite toDomain(JpaBookFavoriteEntity entity);

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "bookId", source = "book.id")
    JpaBookFavoriteEntity toEntity(BookFavorite domain);
}
