package com.backend.application.dto.response.progress;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

// Phản hồi tiến độ nghe cuốn sách của khách hàng
public record BookProgressResponse(
    Long bookId,
    boolean hasProgress,
    List<ChapterProgress> chapters,
    int chapterCount,
    int completedChapterCount,
    boolean isBookComplete,
    @JsonInclude(JsonInclude.Include.ALWAYS) Resume resume) {

    // Tiến độ nghe chi tiết của một chương
    public record ChapterProgress(
        Long chapterId,
        int chapterIndex,
        int lastPlayAt,
        boolean isComplete,
        Instant updatedAt) {

    }

    // Vị trí gợi ý nghe tiếp theo của cuốn sách
    public record Resume(Long chapterId, int chapterIndex, int lastPlayAt) {

    }
}
