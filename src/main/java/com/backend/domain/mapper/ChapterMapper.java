package com.backend.domain.mapper;

import com.backend.domain.model.Chapter;
import com.backend.infrastructure.persistence.entity.JpaChapterEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {BookMapper.class, AttachFileMapper.class})
public interface ChapterMapper {

    Chapter toDomain(JpaChapterEntity entity);

    @Mapping(target = "bookId", source = "book.id")
    @Mapping(target = "attachFileId", source = "attachFile.id")
    JpaChapterEntity toEntity(Chapter domain);
}
