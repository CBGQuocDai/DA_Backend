package com.backend.application.adapter.repository;

import com.backend.domain.model.ListenProgress;
import java.util.List;
import java.util.Optional;

public interface ListenProgressRepository {
    // Lấy danh sách tiến độ nghe tất cả các chương trong cuốn sách của khách hàng
    List<ListenProgress> findByCustomerIdAndBookId(Long customerId, Long bookId);

    // Lấy tiến độ nghe một chương cụ thể của khách hàng
    Optional<ListenProgress> findByCustomerIdAndChapterId(Long customerId, Long chapterId);

    // Lưu hoặc cập nhật bản ghi tiến độ nghe
    ListenProgress save(ListenProgress progress);
}
