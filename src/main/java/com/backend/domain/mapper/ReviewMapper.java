package com.backend.domain.mapper;

import com.backend.domain.model.Review;
import com.backend.infrastructure.persistence.entity.JpaReviewEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {BookMapper.class, UserMapper.class})
public interface ReviewMapper {

    Review toDomain(JpaReviewEntity entity);

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "bookId", source = "book.id")
    JpaReviewEntity toEntity(Review domain);
}

