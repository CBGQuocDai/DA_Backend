package com.backend.domain.mapper;

import com.backend.domain.model.Admin;
import com.backend.infrastructure.persistence.entity.JpaAdminEntity;
import org.springframework.stereotype.Component;

@Component
public class AdminMapper {

    public Admin toDomain(JpaAdminEntity entity) {
        if (entity == null) return null;
        return Admin.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .fullName(entity.getFullName())
                .avatar(entity.getAvatarUrl() != null
                        ? com.backend.domain.model.MediaFile.builder().path(entity.getAvatarUrl()).build()
                        : null)
                .build();
    }
}
