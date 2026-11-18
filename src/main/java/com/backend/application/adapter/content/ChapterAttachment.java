package com.backend.application.adapter.content;

import java.util.List;

/**
 * Tệp đính kèm (ảnh hoặc bảng) trong tài liệu của chương.
 * {@code caption} do admin nhập trong editor, rỗng nếu chưa có.
 */
public sealed interface ChapterAttachment {

    String caption();

    /** @param path đường dẫn ảnh trên storage */
    record Image(String path, String caption) implements ChapterAttachment {
    }

    record Table(List<List<String>> rows, int headerRows, String caption) implements ChapterAttachment {
    }
}
