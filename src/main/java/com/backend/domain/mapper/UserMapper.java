package com.backend.domain.mapper;

import com.backend.domain.model.Admin;
import com.backend.domain.model.Customer;
import com.backend.domain.model.MediaFile;
import com.backend.domain.model.User;
import com.backend.infrastructure.persistence.entity.JpaAdminEntity;
import com.backend.infrastructure.persistence.entity.JpaCustomerEntity;
import com.backend.infrastructure.persistence.entity.JpaUserEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    Admin toAdmin(JpaAdminEntity entity);

    Customer toCustomer(JpaCustomerEntity entity);

    default User toDomain(JpaUserEntity entity) {
        if (entity == null) return null;
        if (entity instanceof JpaAdminEntity) return toAdmin((JpaAdminEntity) entity);
        if (entity instanceof JpaCustomerEntity) return toCustomer((JpaCustomerEntity) entity);
        return null;
    }

    default MediaFile toAvatar(String url) {
        if (url == null) return null;
        return MediaFile.builder().path(url).build();
    }
}
