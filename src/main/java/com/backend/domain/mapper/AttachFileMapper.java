package com.backend.domain.mapper;

import com.backend.domain.model.AttachFile;
import com.backend.infrastructure.persistence.entity.JpaAttachFileEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AttachFileMapper {

    AttachFile toDomain(JpaAttachFileEntity entity);

    JpaAttachFileEntity toEntity(AttachFile domain);
}
