package com.backend.application.service;

import com.backend.application.service.impl.ChapterServiceImpl;
import com.backend.application.adapter.messaging.MessageQueue;
import com.backend.application.adapter.repository.BookRepository;
import com.backend.application.adapter.repository.ChapterRepository;
import com.backend.application.adapter.storage.StorageService;
import com.backend.application.dto.request.book.chapter.ChapterGenerateAudioRequest;
import com.backend.application.adapter.messaging.GenerateAudioRequest;
import com.backend.application.dto.request.book.chapter.GenerateAudioResult;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Book;
import com.backend.domain.model.Chapter;
import com.backend.domain.model.Voice;
import com.backend.domain.valueobject.BusinessError;
import com.backend.domain.valueobject.ChapterStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChapterServiceImplTest {

    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private MessageQueue messageQueue;

    @InjectMocks
    private ChapterServiceImpl chapterService;

    private Book book;
    private Chapter chapter;

    @BeforeEach
    void setUp() {
        Voice voice = Voice.builder().id(5L).exampleAudio("voice-example/abc.wav").build();
        book = Book.builder().id(1L).voice(voice).build();
        chapter = Chapter.builder().id(12L).rawText("Nội dung").build();
    }

    private ChapterGenerateAudioRequest request(Double speed) {
        return ChapterGenerateAudioRequest.builder().bookId(1L).chapterId(12L).speed(speed).build();
    }


//    test generate audio
    @Test
    void generateAudio_bookNotFound_throwsAndDoesNotSend() {
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> chapterService.generateAudio(request(1.5)));

        assertEquals(BusinessError.BOOK_NOT_FOUND, ex.getError());
        verifyNoInteractions(messageQueue);
    }

    @Test
    void generateAudio_chapterNotFound_throwsAndDoesNotSend() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(chapterRepository.findById(12L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> chapterService.generateAudio(request(1.5)));

        assertEquals(BusinessError.CHAPTER_NOT_FOUND, ex.getError());
        verifyNoInteractions(messageQueue);
    }

    @Test
    void generateAudio_alreadyProcessing_throwsConflict() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(chapterRepository.findById(12L)).thenReturn(Optional.of(chapter));
        when(chapterRepository.markProcessingAudio(12L, 1.5)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> chapterService.generateAudio(request(1.5)));

        assertEquals(BusinessError.CHAPTER_AUDIO_PROCESSING, ex.getError());
        verifyNoInteractions(messageQueue);
    }

    @Test
    void generateAudio_sendsJob() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(chapterRepository.findById(12L)).thenReturn(Optional.of(chapter));
        when(chapterRepository.markProcessingAudio(12L, 1.5)).thenReturn(true);

        chapterService.generateAudio(request(1.5));

        GenerateAudioRequest job = captureJob();
        assertEquals(12L, job.getChapterId());
        assertEquals("Nội dung", job.getText());
        assertEquals("voice-example/abc.wav", job.getExampleVoiceUrl());
        assertEquals(1.5, job.getSpeed());
    }

    @Test
    void generateAudio_nullSpeed_defaultsToOne() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(chapterRepository.findById(12L)).thenReturn(Optional.of(chapter));
        when(chapterRepository.markProcessingAudio(12L, 1.0)).thenReturn(true);

        chapterService.generateAudio(request(null));

        verify(chapterRepository).markProcessingAudio(12L, 1.0);
        assertEquals(1.0, captureJob().getSpeed());
    }

    @Test
    void generateAudio_bookWithoutVoice_sendsEmptyVoiceUrl() {
        book.setVoice(null);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(chapterRepository.findById(12L)).thenReturn(Optional.of(chapter));
        when(chapterRepository.markProcessingAudio(12L, 1.0)).thenReturn(true);

        chapterService.generateAudio(request(null));

        assertEquals("", captureJob().getExampleVoiceUrl());
    }

    @Test
    void generateAudio_blankText_throwsAndDoesNotMark() {
        chapter.setRawText("  ");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(chapterRepository.findById(12L)).thenReturn(Optional.of(chapter));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> chapterService.generateAudio(request(1.5)));

        assertEquals(BusinessError.CHAPTER_TEXT_EMPTY, ex.getError());
        verify(chapterRepository, never()).markProcessingAudio(any(), any());
        verifyNoInteractions(messageQueue);
    }

    private Chapter processingChapter() {
        return Chapter.builder().id(12L).status(ChapterStatus.PROCESSING_AUDIO)
                .audioUrl("books/1/audio/old.wav").duration(100).build();
    }

    @Test
    void handleAudioResult_success_marksReady() {
        when(chapterRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(processingChapter()));

        chapterService.handleAudioResult(GenerateAudioResult.builder().chapterId(12L).success(true)
                .audioPath("books/1/audio/12.wav").duration(314).build());

        verify(chapterRepository).updateAudio(12L, "books/1/audio/12.wav", 314, ChapterStatus.AUDIO_READY);
        verify(chapterRepository, never()).update(any());
    }

    @Test
    void handleAudioResult_failure_marksFailedKeepsOldAudio() {
        when(chapterRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(processingChapter()));

        chapterService.handleAudioResult(GenerateAudioResult.builder().chapterId(12L).success(false)
                .error("OOM").build());

        verify(chapterRepository).updateAudio(12L, "books/1/audio/old.wav", 100, ChapterStatus.FAILED);
    }

    @Test
    void handleAudioResult_successWithoutPath_marksFailed() {
        when(chapterRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(processingChapter()));

        chapterService.handleAudioResult(GenerateAudioResult.builder().chapterId(12L).success(true)
                .audioPath(null).duration(314).build());

        verify(chapterRepository).updateAudio(12L, "books/1/audio/old.wav", 100, ChapterStatus.FAILED);
    }

    @Test
    void handleAudioResult_chapterMissing_ignored() {
        when(chapterRepository.findByIdForUpdate(12L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> chapterService.handleAudioResult(
                GenerateAudioResult.builder().chapterId(12L).success(true).audioPath("x.wav").build()));

        verify(chapterRepository, never()).updateAudio(any(), any(), any(), any());
    }

    @Test
    void handleAudioResult_notProcessing_ignored() {
        Chapter ready = processingChapter();
        ready.setStatus(ChapterStatus.AUDIO_READY);
        when(chapterRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(ready));

        chapterService.handleAudioResult(GenerateAudioResult.builder().chapterId(12L).success(true)
                .audioPath("x.wav").build());

        verify(chapterRepository, never()).updateAudio(any(), any(), any(), any());
    }

    @Test
    void handleAudioResult_nullChapterId_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> chapterService.handleAudioResult(GenerateAudioResult.builder().success(true).build()));

        verify(chapterRepository, never()).findByIdForUpdate(any());
    }

    private GenerateAudioRequest captureJob() {
        ArgumentCaptor<GenerateAudioRequest> captor = ArgumentCaptor.forClass(GenerateAudioRequest.class);
        verify(messageQueue).send(captor.capture());
        return captor.getValue();
    }
}
