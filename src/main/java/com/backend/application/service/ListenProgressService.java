package com.backend.application.service;

import com.backend.application.dto.request.progress.ListenProgressRequest;
import com.backend.application.dto.response.progress.BookProgressResponse;

public interface ListenProgressService {
    // Lấy tiến độ nghe của độc giả theo sách
    BookProgressResponse getProgress(Long customerId, Long bookId);

    // Lưu hoặc cập nhật tiến độ nghe của độc giả cho một chương
    BookProgressResponse saveProgress(Long customerId, Long bookId, Long chapterId, ListenProgressRequest request);
}
