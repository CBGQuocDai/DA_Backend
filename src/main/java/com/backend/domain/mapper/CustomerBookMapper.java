package com.backend.domain.mapper;

import com.backend.domain.model.CustomerBook;
import com.backend.infrastructure.persistence.entity.JpaCustomerBookEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {BookMapper.class, UserMapper.class})
public interface CustomerBookMapper {

    CustomerBook toDomain(JpaCustomerBookEntity entity);

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "bookId", source = "book.id")
    JpaCustomerBookEntity toEntity(CustomerBook domain);
}

