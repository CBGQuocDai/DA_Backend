package com.backend.domain.mapper;

import com.backend.domain.model.Category;
import com.backend.infrastructure.persistence.entity.JpaCategoryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    Category toDomain(JpaCategoryEntity entity);

    JpaCategoryEntity toEntity(Category domain);
}
