package com.backend.infrastructure.persistence.mapper;

import com.backend.domain.model.Book;
import com.backend.infrastructure.persistence.entity.JpaBookEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {VoiceMapper.class})
public interface BookMapper {

    // Các danh sách liên quan được nạp riêng tại repository adapter.
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "authors", ignore = true)
    @Mapping(target = "chapters", ignore = true)
    Book toDomain(JpaBookEntity entity);

    @Mapping(target = "voiceId", source = "voice.id")
    JpaBookEntity toEntity(Book domain);
}

