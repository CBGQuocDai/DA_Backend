package com.backend.domain.mapper;

import com.backend.domain.model.Customer;
import com.backend.infrastructure.persistence.entity.JpaCustomerEntity;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public Customer toDomain(JpaCustomerEntity entity) {
        if (entity == null) return null;
        return Customer.builder()
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
