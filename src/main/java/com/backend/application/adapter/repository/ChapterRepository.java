package com.backend.application.adapter.repository;

import com.backend.domain.model.Chapter;
import java.util.List;
import java.util.Optional;

public interface ChapterRepository {
    /** Lấy các chương của sách theo thứ tự chương tăng dần. */
    List<Chapter> findByBookId(Long bookId);

    /** Lấy chương theo ID và giữ khóa ghi đến hết giao dịch; trả về rỗng nếu không tồn tại. */
    Optional<Chapter> findById(Long id);

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
