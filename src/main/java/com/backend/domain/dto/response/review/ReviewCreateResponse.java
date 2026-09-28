package com.backend.domain.dto.response.review;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCreateResponse {

    private Long id;
    private Double rate;
    private String content;
    private Instant createAt;
    private Long customerId;
    private Long bookId;
}
