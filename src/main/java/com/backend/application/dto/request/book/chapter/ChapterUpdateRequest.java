package com.backend.application.dto.request.book.chapter;

import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterUpdateRequest {
    private String title;
    /** Tài liệu biên tập của chương; null thì giữ nguyên nội dung. */
    private String content;
    @Min(value = 0, message = "Thứ tự chương không được âm")
    private Integer chapterOrder;
}
