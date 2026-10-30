package com.backend.domain.dto.response.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private Long bookId;
    private Double rate;
    private Double rating;
    private String content;
    private String comment;
    private Instant createAt;
    private Instant createdAt;
    private Long customerId;
    private Long userId;
    private String customerName;
    private String userName;
    private String customerAvatar;
    private String userAvatar;
}
