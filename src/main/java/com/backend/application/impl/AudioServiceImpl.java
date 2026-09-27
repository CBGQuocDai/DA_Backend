package com.backend.application.impl;

import com.backend.application.AudioService;
import com.backend.application.VoiceService;
import com.backend.domain.adapter.repository.ChapterRepository;
import com.backend.domain.adapter.storage.StorageService;
import com.backend.domain.dto.response.AudioGenerationResponse;
import com.backend.domain.model.Chapter;
import com.backend.domain.valueobject.ChapterStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AudioServiceImpl implements AudioService {

    private final ChapterRepository chapterRepository;
    private final VoiceService voiceService;
    private final StorageService storageService;

    @Override
    @Transactional
    public AudioGenerationResponse generateAudio(Long bookId, Long chapterId) {
        Chapter chapter = getChapter(bookId, chapterId);

        // Update status to PROCESSING_AUDIO
        chapter.setStatus(ChapterStatus.PROCESSING_AUDIO);
        chapterRepository.save(chapter);

        // Get chapter text for TTS
        String text = chapter.getRawText();
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Chapter has no text to convert");
        }

        // Call external TTS service (placeholder - VoiceService would need TTS methods)
        // For now, generate a placeholder audio URL
        String audioUrl = generateTtsAudio(text, chapterId);
        int duration = estimateDuration(text);

        // Update chapter with audio info
        chapter.setAudioUrl(audioUrl);
        chapter.setDuration(duration);
        chapter.setStatus(ChapterStatus.AUDIO_READY);
        chapterRepository.save(chapter);

        return AudioGenerationResponse.builder()
                .chapterId(chapterId)
                .audioUrl(audioUrl)
                .duration(duration)
                .status(ChapterStatus.AUDIO_READY)
                .build();
    }

    @Override
    @Transactional
    public AudioGenerationResponse uploadAudio(Long bookId, Long chapterId, MultipartFile file) {
        Chapter chapter = getChapter(bookId, chapterId);

        // Save file to GCS
        String filePath = String.format("books/%d/chapters/%d/audio/%s",
                bookId, chapterId, file.getOriginalFilename());
        storageService.store(file, filePath);

        // Create signed URL for the uploaded file
        String audioUrl = storageService.createSignUrl(filePath, 60L);

        // Estimate duration from file (placeholder - actual implementation would analyze file)
        int duration = estimateDurationFromFile(file);

        // Update chapter with audio info
        chapter.setAudioUrl(audioUrl);
        chapter.setDuration(duration);
        chapter.setStatus(ChapterStatus.AUDIO_READY);
        chapterRepository.save(chapter);

        return AudioGenerationResponse.builder()
                .chapterId(chapterId)
                .audioUrl(audioUrl)
                .duration(duration)
                .status(ChapterStatus.AUDIO_READY)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ChapterStatus getAudioStatus(Long chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new IllegalArgumentException("Chapter not found with id: " + chapterId));
        return chapter.getStatus();
    }

    private Chapter getChapter(Long bookId, Long chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new IllegalArgumentException("Chapter not found with id: " + chapterId));

        if (!chapter.getBook().getId().equals(bookId)) {
            throw new IllegalArgumentException("Chapter does not belong to the specified book");
        }

        return chapter;
    }

    private String generateTtsAudio(String text, Long chapterId) {
        // Placeholder for TTS generation
        // In a real implementation, this would call an external TTS API
        // such as Google Cloud Text-to-Speech, AWS Polly, or similar
        return String.format("/generated/audio/chapter_%d.mp3", chapterId);
    }

    private int estimateDuration(String text) {
        // Rough estimate: ~150 words per minute
        // Average word length is ~5 characters, so ~750 characters per minute
        int words = text.split("\\s+").length;
        return Math.max(1, words * 60 / 150);
    }

    private int estimateDurationFromFile(MultipartFile file) {
        // Placeholder for actual audio duration calculation
        // In a real implementation, this would analyze the audio file
        // For now, estimate based on file size (assuming ~128kbps)
        long bytes = file.getSize();
        return (int) (bytes * 8 / 128 / 1000);
    }
}
