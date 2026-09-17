package com.backend.domain.mapper;

import com.backend.domain.model.Review;
import com.backend.infrastructure.persistence.entity.JpaReviewEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {BookMapper.class, CustomerMapper.class})
public interface ReviewMapper {

    @Mapping(target = "customer", ignore = true)
    Review toDomain(JpaReviewEntity entity);

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "bookId", source = "book.id")
    JpaReviewEntity toEntity(Review domain);
}
