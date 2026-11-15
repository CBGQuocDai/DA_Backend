package com.backend.application.dto.request.progress;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// DTO yêu cầu cập nhật tiến độ nghe chương
@Getter
@Setter
public class ListenProgressRequest {
    @NotNull(message = "Vị trí nghe không được để trống")
    @Min(value = 0, message = "Vị trí nghe phải lớn hơn hoặc bằng 0")
    private Integer lastPlayAt;
    private Boolean isComplete;
}
