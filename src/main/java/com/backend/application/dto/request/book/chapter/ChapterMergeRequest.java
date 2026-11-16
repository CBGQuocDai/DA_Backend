package com.backend.application.dto.request.book.chapter;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** Các chương liền nhau cần gộp; chương có thứ tự nhỏ nhất được giữ lại. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterMergeRequest {
    @NotNull(message = "Danh sách chương không được để trống")
    @Size(min = 2, message = "Cần chọn ít nhất 2 chương để gộp")
    private List<@NotNull Long> chapterIds;
}
