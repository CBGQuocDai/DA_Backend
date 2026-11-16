package com.backend.application.adapter.content;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Đọc tài liệu biên tập được lưu cho một chương.
 * Đây là port interface; định dạng của tài liệu không lộ ra tầng application.
 */
public interface ChapterContentParser {
    /**
     * Văn bản thuần của tài liệu: mỗi block một dòng, khớp với văn bản mà editor trả về.
     *
     * @throws com.backend.domain.exception.BusinessException nếu tài liệu không hợp lệ
     */
    String extractText(String contentJson);

    /**
     * Tài liệu với URL hiển thị của từng ảnh được gắn theo đường dẫn trên storage, hoặc bị bỏ đi
     * khi {@code urlOfPath} trả về null. URL có hạn dùng nên được gắn khi đọc và bỏ đi khi lưu.
     *
     * @throws com.backend.domain.exception.BusinessException nếu tài liệu không hợp lệ
     */
    String withImageUrls(String contentJson, UnaryOperator<String> urlOfPath);

    /**
     * Ảnh và bảng của tài liệu theo thứ tự xuất hiện. Không ném lỗi: tài liệu trống hoặc không hợp lệ
     * cho danh sách rỗng, ảnh có đường dẫn không hợp lệ bị bỏ qua.
     */
    List<ChapterAttachment> attachments(String contentJson);

    /**
     * Gộp nội dung nhiều chương theo thứ tự: mỗi phần mở đầu bằng dòng {@code heading} (nếu có) rồi tới nội dung.
     * Tài liệu biên tập chỉ được tạo khi có phần mang tài liệu; khi đó văn bản thuần được suy ra từ nó.
     *
     * @throws com.backend.domain.exception.BusinessException nếu một tài liệu không hợp lệ
     */
    ChapterContent merge(List<ChapterContent> parts);
}
