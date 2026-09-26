package com.backend.domain.dto.request.user.customer;

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
public class CustomerChangePasswordRequest {

    private String oldPassword;
    private String newPassword;
}
