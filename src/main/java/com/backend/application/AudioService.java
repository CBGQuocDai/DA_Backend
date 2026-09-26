package com.backend.application;

import com.backend.domain.dto.response.AudioGenerationResponse;
import com.backend.domain.valueobject.ChapterStatus;
import org.springframework.web.multipart.MultipartFile;

public interface AudioService {
    AudioGenerationResponse generateAudio(Long bookId, Long chapterId);
    AudioGenerationResponse uploadAudio(Long bookId, Long chapterId, MultipartFile file);
    ChapterStatus getAudioStatus(Long chapterId);
}
