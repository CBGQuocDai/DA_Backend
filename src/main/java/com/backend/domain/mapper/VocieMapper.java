package com.backend.domain.mapper;

import com.backend.domain.model.Voice;
import com.backend.infrastructure.persistence.entity.JpaVoiceEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VocieMapper {

    Voice toDomain(JpaVoiceEntity entity);

    JpaVoiceEntity toEntity(Voice domain);
}
