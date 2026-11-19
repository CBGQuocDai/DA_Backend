package com.backend.application.adapter.content;

import com.backend.domain.model.Chapter;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Các thao tác trên tài liệu biên tập ({@code Chapter.content}) của chương.
 * Đây là port interface; định dạng của tài liệu không lộ ra tầng application.
 * Tài liệu null hoặc rỗng được coi là chương chưa có nội dung.
 */
public interface ChapterContentService {

    /** Thư mục trên storage mà ảnh của tài liệu bắt buộc phải nằm trong. */
    String IMAGE_PATH_PREFIX = "images/";

    /** Bộ dựng một tài liệu mới, rỗng. */
    ChapterContentBuilder newBuilder();

    /**
     * Tài liệu editor gửi lên, đã kiểm tra và bỏ URL ký tạm của ảnh, sẵn sàng để lưu; null nếu rỗng.
     *
     * @throws com.backend.domain.exception.BusinessException nếu tài liệu không hợp lệ
     */
    String sanitize(String content);

    /**
     * Văn bản TTS đọc: mỗi block một dòng, ảnh và bảng thành câu trỏ tới tệp đính kèm, bỏ dấu ngắt trang.
     *
     * @throws com.backend.domain.exception.BusinessException nếu tài liệu không hợp lệ
     */
    String text(String content);

    /**
     * Tài liệu cho editor: ảnh được gắn URL hiển thị theo đường dẫn trên storage, hoặc bỏ URL đi khi
     * {@code urlOfPath} trả về null. URL có hạn dùng nên được gắn khi đọc và bỏ đi khi lưu.
     *
     * @throws com.backend.domain.exception.BusinessException nếu tài liệu không hợp lệ
     */
    String withImageUrls(String content, UnaryOperator<String> urlOfPath);

    /**
     * Ảnh và bảng của chương theo thứ tự xuất hiện; rỗng nếu chương chưa có nội dung.
     *
     * @throws com.backend.domain.exception.BusinessException nếu tài liệu không hợp lệ
     */
    List<ChapterAttachment> attachments(String content);

    /**
     * Tài liệu gộp nội dung các chương theo thứ tự: từ chương thứ hai, mỗi chương mở đầu bằng dòng tiêu đề của nó.
     *
     * @throws com.backend.domain.exception.BusinessException nếu một tài liệu không hợp lệ
     */
    String merge(List<Chapter> chapters);
}
