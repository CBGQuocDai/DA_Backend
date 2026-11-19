package com.backend.domain.valueobject;

/**
 * Dữ liệu rút gọn phục vụ phát audio: tránh tải nội dung văn bản nặng của chương.
 */
public record PlaybackChapter(
    Long id,
    String title,
    Integer chapterOrder,
    Integer duration,
    String audioUrl,
    ChapterStatus status) {

    // Trạng thái sẵn sàng chỉ có ý nghĩa khi audio và thời lượng đều hợp lệ.
    public boolean isPlayable() {
        return status == ChapterStatus.AUDIO_READY && duration != null && duration > 0
            && audioUrl != null && !audioUrl.isBlank();
    }
}
