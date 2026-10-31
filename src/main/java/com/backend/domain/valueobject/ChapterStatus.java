package com.backend.domain.valueobject;

public enum ChapterStatus {
    FAILED, //generate audio lỗi
    PROCESSING_AUDIO,  // Đang generate audio
    AUDIO_READY        // Audio đã sẵn sàng
}
