package com.backend.domain.valueobject;

public enum BookStatus {
    PROCESSING,      // Đang OCR
    UNPUBLISHED,    // Ocr xong, sẵn sàng để chỉnh sửa. chưa được publish cho khách hàng
    PUBLISHED       // Đã publish
}
