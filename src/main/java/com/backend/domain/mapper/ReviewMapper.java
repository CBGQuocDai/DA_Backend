package com.backend.domain.mapper;

import com.backend.domain.dto.response.review.ReviewResponse;
import com.backend.domain.model.Review;
import com.backend.infrastructure.persistence.entity.JpaReviewEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {BookMapper.class, CustomerMapper.class})
public interface ReviewMapper {

    Review toDomain(JpaReviewEntity entity);

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "bookId", source = "book.id")
    JpaReviewEntity toEntity(Review domain);

    default ReviewResponse toResponse(Review domain) {
        if (domain == null) {
            return null;
        }
        Long customerId = domain.getCustomer() != null ? domain.getCustomer().getId() : null;
        String customerName = domain.getCustomer() != null ? domain.getCustomer().getFullName() : null;
        String customerAvatar = domain.getCustomer() != null ? domain.getCustomer().getAvatarUrl() : null;
        Long bookId = domain.getBook() != null ? domain.getBook().getId() : null;

        return ReviewResponse.builder()
                .id(domain.getId())
                .bookId(bookId)
                .rate(domain.getRate())
                .rating(domain.getRate())
                .content(domain.getContent())
                .comment(domain.getContent())
                .createAt(domain.getCreateAt())
                .createdAt(domain.getCreateAt())
                .customerId(customerId)
                .userId(customerId)
                .customerName(customerName)
                .userName(customerName)
                .customerAvatar(customerAvatar)
                .userAvatar(customerAvatar)
                .build();
    }
}
