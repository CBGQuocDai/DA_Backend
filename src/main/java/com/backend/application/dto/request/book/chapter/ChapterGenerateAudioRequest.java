package com.backend.application.dto.request.book.chapter;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterGenerateAudioRequest {
    @NotNull(message = "bookId không được để trống")
    private Long bookId;

    @NotNull(message = "chapterId không được để trống")
    private Long chapterId;

    // Reading-speed multiplier, 1.0 = normal; null falls back to 1.0
    @DecimalMin(value = "0.5", message = "Tốc độ đọc phải nằm trong khoảng từ 0.5 đến 2.0")
    @DecimalMax(value = "2.0", message = "Tốc độ đọc phải nằm trong khoảng từ 0.5 đến 2.0")
    private Double speed;
}
