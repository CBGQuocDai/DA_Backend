package com.backend.application.dto.request.review;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
public class ReviewCreateRequest {

    @NotNull(message = "Sách không được để trống")
    @Positive(message = "Mã sách không hợp lệ")
    private Long bookId;

    @NotNull(message = "Vui lòng chọn số sao")
    @DecimalMin(value = "1", message = "Số sao phải từ 1 đến 5")
    @DecimalMax(value = "5", message = "Số sao phải từ 1 đến 5")
    @Digits(integer = 1, fraction = 0, message = "Số sao phải là số nguyên")
    private Double rate;

    @NotBlank(message = "Vui lòng viết nhận xét")
    @Size(max = 1000, message = "Nhận xét không được quá 1000 ký tự")
    private String content;
}
