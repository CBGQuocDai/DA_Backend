package com.backend.application.adapter.ocr;

import java.util.List;

/** Một phần của sách mà OCR nhận dạng được, theo thứ tự đọc. {@link #page()} tính từ 1 trên cả file PDF. */
public sealed interface OcrBlock {

    int page();

    enum TextKind {
        HEADING,
        BODY,
        /** Đầu trang lặp lại ở mọi trang. */
        HEADER,
        /** Chân trang; đánh dấu một trang đã kết thúc. */
        FOOTER
    }

    record Text(int page, TextKind kind, String text) implements OcrBlock {
    }

    /** @param description mô tả do OCR sinh ra, rỗng nếu không có */
    record Image(int page, byte[] content, String mimeType, String description) implements OcrBlock {
    }

    /** @param rows các hàng header trước, rồi tới các hàng thân bảng */
    record Table(int page, List<List<String>> rows, int headerRows, String description) implements OcrBlock {
    }
}
