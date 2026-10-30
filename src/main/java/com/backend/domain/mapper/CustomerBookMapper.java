package com.backend.domain.mapper;

import com.backend.domain.model.CustomerBook;
import com.backend.infrastructure.persistence.entity.JpaCustomerBookEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerBookMapper {

    CustomerBook toDomain(JpaCustomerBookEntity entity);

    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "book", ignore = true)
    JpaCustomerBookEntity toEntity(CustomerBook domain);
}