package com.backend.application.adapter.repository;

import com.backend.domain.model.Chapter;
import com.backend.domain.valueobject.PlaybackChapter;
import java.util.List;
import java.util.Optional;

public interface ChapterRepository {
    // Lấy danh sách chương rút gọn theo sách để phục vụ trình phát audio
    List<PlaybackChapter> findPlaybackChaptersByBookId(Long bookId);

    /** Lấy các chương của sách theo thứ tự chương tăng dần. */
    List<Chapter> findByBookId(Long bookId);

    /** Lấy chương theo ID và giữ khóa ghi đến hết giao dịch; trả về rỗng nếu không tồn tại. */
    Optional<Chapter> findById(Long id);

    /** Lấy tài liệu của chương mà không khóa; rỗng nếu chương không tồn tại hoặc chưa có nội dung. */
    Optional<String> findContentById(Long id);

    /** Lấy thứ tự chương lớn nhất của sách; trả về rỗng nếu chưa có chương. */
    Optional<Integer> findMaxChapterOrder(Long bookId);

    /** Dịch thứ tự các chương trong đoạn [from, to] của sách thêm delta. */
    void shiftOrders(Long bookId, int from, int to, int delta);

    /** Lưu chương mới; domain entity phải có bookId. */
    Chapter save(Chapter chapter);

    /**
     * Lưu danh sách chương mới; mỗi domain entity phải có bookId.
     */
    void saveAll(List<Chapter> chapters);

    /** Cập nhật nội dung, thứ tự, audio, tốc độ đọc và trạng thái từ domain entity. */
    Chapter update(Chapter chapter);

    /** Xóa chương theo ID. */
    void deleteById(Long id);
}
