package com.backend.application.adapter.messaging;

import java.util.Optional;

/** Mã job TTS đang chạy của từng chương; kết quả mang mã khác (job đã hủy hoặc đã bị thay) bị bỏ qua. */
public interface JobRegistry {
    void put(Long chapterId, String jobId);

    Optional<String> find(Long chapterId);

    void remove(Long chapterId);
}
