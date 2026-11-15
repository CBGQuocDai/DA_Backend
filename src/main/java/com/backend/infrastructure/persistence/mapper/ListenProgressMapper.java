package com.backend.infrastructure.persistence.mapper;

import com.backend.domain.model.ListenProgress;
import com.backend.infrastructure.persistence.entity.JpaListenProgressEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ListenProgressMapper {
    // Chuyển đổi từ JPA Entity sang Domain Model
    ListenProgress toDomain(JpaListenProgressEntity entity);

    // Chuyển đổi từ Domain Model sang JPA Entity
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "book", ignore = true)
    @Mapping(target = "chapter", ignore = true)
    JpaListenProgressEntity toEntity(ListenProgress progress);
}
