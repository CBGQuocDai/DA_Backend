package com.backend.domain.mapper;

import com.backend.domain.model.Customer;
import com.backend.infrastructure.persistence.entity.JpaCustomerEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface CustomerMapper {

    Customer toDomain(JpaCustomerEntity entity);

    JpaCustomerEntity toEntity(Customer domain);
}
