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
public class CustomerUpdateAvatarRequest {
    @NotBlank(message = "Đường dẫn ảnh không được để trống")
    private String avatarPath;
}
