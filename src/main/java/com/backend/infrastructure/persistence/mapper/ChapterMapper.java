package com.backend.infrastructure.persistence.mapper;

import com.backend.domain.model.Chapter;
import com.backend.infrastructure.persistence.entity.JpaChapterEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {BookMapper.class, AttachFileMapper.class})
public interface ChapterMapper {

    Chapter toDomain(JpaChapterEntity entity);

    // Quan hệ book chỉ dùng để đọc; khóa ngoại được ánh xạ từ bookId của domain.
    @Mapping(target = "book", ignore = true)
    @Mapping(target = "attachFileId", source = "attachFile.id")
    JpaChapterEntity toEntity(Chapter domain);
}
