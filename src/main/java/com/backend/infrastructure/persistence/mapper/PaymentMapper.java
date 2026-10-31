package com.backend.infrastructure.persistence.mapper;

import com.backend.domain.model.Payment;
import com.backend.infrastructure.persistence.entity.JpaPaymentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    Payment toDomain(JpaPaymentEntity entity);

    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "book", ignore = true)
    JpaPaymentEntity toEntity(Payment domain);
}
