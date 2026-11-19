package com.backend.domain.model;

import com.backend.domain.valueobject.ChapterStatus;
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
public class Chapter {
    private Long id;
    private Long bookId;
    private String title;
    private Integer chapterOrder;
    private Integer duration; // seconds
    private String audioUrl;
    /** Tài liệu biên tập (JSON) của chương; văn bản cho TTS và tệp đính kèm đều lấy từ đây. Null khi chưa có nội dung. */
    private String content;
    private Double readingSpeed;
    private ChapterStatus status;
    /** Mã job TTS đang chạy; kết quả mang mã khác (job đã hủy hoặc đã bị thay) bị bỏ qua. */
    private String audioJobId;
}
