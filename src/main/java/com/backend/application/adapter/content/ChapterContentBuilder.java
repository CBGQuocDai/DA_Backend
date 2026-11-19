package com.backend.application.adapter.content;

import java.util.List;

/** Dựng dần tài liệu biên tập ({@code Chapter.content}) của một chương; định dạng tài liệu do adapter quyết định. */
public interface ChapterContentBuilder {

    void paragraph(String text);

    void pageBreak();

    /** @param path đường dẫn ảnh trên storage, nằm trong {@link ChapterContentService#IMAGE_PATH_PREFIX} */
    void image(String path, String description);

    void table(List<List<String>> rows, int headerRows, String description);

    /** Có nội dung nào khác ngoài tệp đính kèm và dòng trống hay không. */
    boolean hasText();

    /** Có ảnh hoặc bảng nào hay không. */
    boolean hasAttachment();

    void clear();

    /** Tài liệu để lưu vào {@code Chapter.content}, bỏ các dòng trống ở đầu và cuối. */
    String build();
}
