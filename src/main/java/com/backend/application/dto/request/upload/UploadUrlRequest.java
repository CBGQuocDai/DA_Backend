package com.backend.application.dto.request.upload;

import com.backend.domain.valueobject.UploadType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadUrlRequest {
    @NotNull(message = "Loại file không được để trống")
    private UploadType type;

    @NotBlank(message = "Tên file không được để trống")
    private String fileName;

    @NotNull(message = "Kích thước file không được để trống")
    @Positive(message = "Kích thước file không hợp lệ")
    private Long size;
}
