package com.backend.application.adapter.realtime;

import com.backend.domain.valueobject.BookStatus;
import com.backend.domain.valueobject.ChapterStatus;

/** Đẩy thay đổi trạng thái tới các trang admin đang mở. Lỗi khi gửi không bao giờ ném ra cho bên gọi. */
public interface AdminEventPublisher {

    /** OCR kết thúc: sách chuyển sang UNPUBLISHED hoặc FAILED. */
    void bookStatusChanged(Long bookId, BookStatus status);

    /** Gen audio kết thúc: chương chuyển sang AUDIO_READY hoặc FAILED. */
    void chapterStatusChanged(Long bookId, Long chapterId, ChapterStatus status, String audioUrl, Integer duration);
}
