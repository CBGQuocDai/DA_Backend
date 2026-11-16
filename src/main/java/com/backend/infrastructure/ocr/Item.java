package com.backend.infrastructure.ocr;

/** Một phần của sách theo thứ tự đọc; số trang bắt đầu lại từ 1 ở mỗi chunk OCR. */
sealed interface Item permits TextItem, ImageItem, TableItem {
    int page();
}
