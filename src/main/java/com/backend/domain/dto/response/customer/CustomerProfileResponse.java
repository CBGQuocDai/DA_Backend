package com.backend.domain.dto.response.customer;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CustomerProfileResponse {
    private Long id;
    private String email;
    private String fullName;
    private String avatarUrl;
}
