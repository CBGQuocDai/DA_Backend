package com.backend.domain.valueobject;

public enum ChapterStatus {
    PENDING_TEXT,      // Chưa có text
    TEXT_READY,        // Text đã sẵn sàng
    PROCESSING_AUDIO,  // Đang generate audio
    AUDIO_READY        // Audio đã sẵn sàng
}
