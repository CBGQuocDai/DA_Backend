package com.backend.domain.mapper;

import com.backend.domain.model.Admin;
import com.backend.infrastructure.persistence.entity.JpaAdminEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface AdminMapper {

    Admin toDomain(JpaAdminEntity entity);

    JpaAdminEntity toEntity(Admin domain);
}
