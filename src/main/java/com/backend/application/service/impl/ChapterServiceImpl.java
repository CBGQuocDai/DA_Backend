package com.backend.application.service.impl;

import com.backend.application.service.ChapterService;
import com.backend.application.adapter.messaging.MessageQueue;
import com.backend.application.adapter.repository.BookRepository;
import com.backend.application.adapter.repository.ChapterRepository;
import com.backend.application.dto.response.chapter.ChapterResponse;
import com.backend.application.mapper.ResponseMapper;
import com.backend.application.dto.request.book.chapter.ChapterGenerateAudioRequest;
import com.backend.application.dto.request.book.chapter.ChapterUpdateRequest;
import com.backend.application.adapter.messaging.GenerateAudioRequest;
import com.backend.application.dto.request.book.chapter.GenerateAudioResult;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Book;
import com.backend.domain.model.Chapter;
import com.backend.domain.valueobject.BusinessError;
import com.backend.domain.valueobject.ChapterStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChapterServiceImpl implements ChapterService {

    private final ChapterRepository chapterRepository;
    private final BookRepository bookRepository;
    private final ResponseMapper responseMapper;
    private final MessageQueue messageQueue;

    @Override
    @Transactional(readOnly = true)
    public List<ChapterResponse> getChaptersByBookId(Long bookId) {
        return chapterRepository.findByBookId(bookId).stream().map(chapter -> {
            ChapterResponse response = responseMapper.toChapterResponse(chapter);
            response.setAudioUrl(responseMapper.signUrl(chapter.getAudioUrl()));
            return response;
        }).toList();
    }

    @Override
    @Transactional
    public void deleteChapter(Long chapterId) {
        chapterRepository.delete(chapterId);
    }

    @Override
    @Transactional
    public void generateAudio(ChapterGenerateAudioRequest req) {
        Book book = bookRepository.findById(req.getBookId()).orElseThrow(
                () -> new BusinessException(BusinessError.BOOK_NOT_FOUND)
        );
        Chapter chapter = chapterRepository.findById(req.getChapterId()).orElseThrow(
                () -> new BusinessException(BusinessError.CHAPTER_NOT_FOUND)
        );
        if (chapter.getRawText() == null || chapter.getRawText().isBlank()) {
            throw new BusinessException(BusinessError.CHAPTER_TEXT_EMPTY);
        }

        double readingSpeed = Objects.requireNonNullElse(req.getSpeed(), 1.0);
        if (!chapterRepository.markProcessingAudio(chapter.getId(), readingSpeed)) {
            throw new BusinessException(BusinessError.CHAPTER_AUDIO_PROCESSING);
        }
        log.info("Queueing audio for book {} with voiceId {} and chapter {} at speed {}",
                book.getId(), book.getVoice() != null ? book.getVoice().getId() : null, chapter.getId(), readingSpeed);
        GenerateAudioRequest job = GenerateAudioRequest.builder()
                .chapterId(chapter.getId())
                .exampleVoiceUrl(book.getVoice() != null ? book.getVoice().getExampleAudio() : "")
                .text(chapter.getRawText())
                .speed(readingSpeed)
                .build();
        messageQueue.send(job);
    }

    @Override
    public void update(Long chapterId, ChapterUpdateRequest req) {
        Chapter chapter = chapterRepository.findById(chapterId).orElseThrow(
                () -> new BusinessException(BusinessError.CHAPTER_NOT_FOUND)
        );
        if(Objects.nonNull(req.getRawText())) {
            chapter.setRawText(req.getRawText());
        }
        if(Objects.nonNull(req.getTitle())) {
            chapter.setTitle(req.getTitle());
        }
        chapterRepository.update(chapter);
    }

    @Override
    @Transactional
    public void handleAudioResult(GenerateAudioResult result) {
        if (result.getChapterId() == null) {
            throw new IllegalArgumentException("TTS result is missing chapterId");
        }
        Chapter chapter = chapterRepository.findByIdForUpdate(result.getChapterId()).orElse(null);
        if (chapter == null) {
            log.warn("TTS result for unknown chapter {}, ignoring", result.getChapterId());
            return;
        }
        if (chapter.getStatus() != ChapterStatus.PROCESSING_AUDIO) {
            log.warn("TTS result for chapter {} in status {}, ignoring", chapter.getId(), chapter.getStatus());
            return;
        }
        if (result.isSuccess() && result.getAudioPath() != null && !result.getAudioPath().isBlank()) {
            chapterRepository.updateAudio(chapter.getId(), result.getAudioPath(), result.getDuration(),
                    ChapterStatus.AUDIO_READY);
        } else {
            log.error("TTS failed for chapter {}: {}", chapter.getId(), result.getError());
            chapterRepository.updateAudio(chapter.getId(), chapter.getAudioUrl(), chapter.getDuration(),
                    ChapterStatus.FAILED);
        }
    }
}
