package com.backend.application.adapter.ocr;

import java.util.List;

/**
 * Port tới dịch vụ OCR: chỉ nhận dạng nội dung PDF. Việc chia chương, lưu ảnh, lưu chương
 * và đổi trạng thái sách thuộc tầng application.
 */
public interface OcrService {

    /**
     * Các block văn bản, ảnh và bảng của file PDF theo thứ tự đọc.
     *
     * @throws IllegalStateException nếu không nhận dạng được file
     */
    List<OcrBlock> recognize(byte[] pdf);
}
