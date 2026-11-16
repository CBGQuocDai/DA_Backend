package com.backend.application.adapter.content;

import java.util.List;

/**
 * Một tệp đính kèm của chương: ảnh hoặc bảng lấy ra từ sách. Ảnh chỉ có {@code imagePath}
 * (đường dẫn trên storage, chưa ký); bảng chỉ có {@code rows} và {@code headerRows}.
 */
public record ChapterAttachment(Type type, String imagePath, List<List<String>> rows, int headerRows) {

    public enum Type {
        IMAGE, TABLE
    }
}
