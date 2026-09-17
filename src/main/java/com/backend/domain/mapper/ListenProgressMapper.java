package com.backend.domain.mapper;

import com.backend.domain.model.ListenProgress;
import com.backend.infrastructure.persistence.entity.JpaListenProgressEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {BookMapper.class, ChapterMapper.class, CustomerMapper.class})
public interface ListenProgressMapper {

    @Mapping(target = "customer", ignore = true)
    ListenProgress toDomain(JpaListenProgressEntity entity);

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "bookId", source = "book.id")
    @Mapping(target = "chapterId", source = "chapter.id")
    JpaListenProgressEntity toEntity(ListenProgress domain);
}
