package com.backend.application.dto.request.user.customer;

import jakarta.validation.constraints.NotBlank;
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
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;
}
