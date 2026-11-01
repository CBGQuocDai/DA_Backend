package com.backend.application.dto.request.book.chapter;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterCreateRequest {
    @NotBlank(message = "Tiêu đề chương không được để trống")
    @Size(max = 255, message = "Tiêu đề chương không được vượt quá 255 ký tự")
    private String title;

    @NotNull(message = "Thứ tự chương không được để trống")
    @Min(value = 0, message = "Thứ tự chương không được âm")
    private Integer chapterOrder;
}
