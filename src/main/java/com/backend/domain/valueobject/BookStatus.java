package com.backend.domain.valueobject;

public enum BookStatus {
    PROCESSING,      // Đang OCR
    READY_FOR_EDIT, // OCR xong, đang edit chapters
    READY,          // Tất cả chapters có audio
    PUBLISHED       // Đã publish
}
