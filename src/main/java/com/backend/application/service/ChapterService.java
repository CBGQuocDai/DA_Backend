package com.backend.application.service;

import com.backend.application.dto.request.book.chapter.ChapterUploadAudioRequest;
import com.backend.application.dto.request.book.chapter.ChapterCreateRequest;
import com.backend.application.dto.request.book.chapter.ChapterGenerateAudioRequest;
import com.backend.application.dto.request.book.chapter.ChapterUpdateRequest;
import com.backend.application.dto.request.book.chapter.GenerateAudioResult;
import com.backend.application.dto.response.chapter.ChapterResponse;
import java.util.List;

public interface ChapterService {
    List<ChapterResponse> getChaptersByBookId(Long bookId);

    void uploadAudio(Long bookId, Long chapterId,
            ChapterUploadAudioRequest request);

    void generateAudio(ChapterGenerateAudioRequest req);

    ChapterResponse createChapter(Long bookId, ChapterCreateRequest req);
    void update(Long bookId, Long chapterId, ChapterUpdateRequest req);
    void deleteChapter(Long chapterId);

    /**
     * Gộp các chương liền nhau vào chương có thứ tự nhỏ nhất: giữ tiêu đề của nó, nối nội dung các chương sau
     * (mỗi chương mở đầu bằng tiêu đề của nó), xóa audio và hủy job TTS đang chạy của các chương đó.
     */
    ChapterResponse mergeChapters(Long bookId, List<Long> chapterIds);


    //    hàm xử lý event khi tts báo gen audio xong
    void handleAudioResult(GenerateAudioResult result);
}
