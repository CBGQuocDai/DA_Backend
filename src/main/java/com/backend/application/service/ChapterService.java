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


    //    hàm xử lý event khi tts báo gen audio xong
    void handleAudioResult(GenerateAudioResult result);
}
