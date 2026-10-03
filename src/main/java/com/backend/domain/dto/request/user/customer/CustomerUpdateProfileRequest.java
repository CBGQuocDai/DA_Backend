package com.backend.domain.dto.request.user.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerUpdateProfileRequest {
    private String fullName;
    private String avatarUrl;
}
