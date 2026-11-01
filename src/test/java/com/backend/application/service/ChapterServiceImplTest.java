package com.backend.application.service;

import com.backend.application.adapter.content.ChapterContentParser;
import com.backend.infrastructure.content.JacksonChapterContentParser;
import com.backend.application.dto.request.book.chapter.ChapterUpdateRequest;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    /** Captured from the Tiptap editor, so the derivation is checked against its real output. */
    private static final String EDITOR_DOC =
            "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"center\"},\"content\":[{\"type\":\"text\",\"text\":\"Tiêu đề “có ngoặc” \\\\ và \\\"kép\\\"\"}]},{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":null}},{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":null},\"content\":[{\"type\":\"text\",\"text\":\"  Dòng thụt đầu, cuối có cách  \"}]},{\"type\":\"pageBreak\"},{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":null},\"content\":[{\"type\":\"text\",\"text\":\"Trang hai — ký tự đặc biệt… №\"}]},{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":null}},{\"type\":\"pageBreak\"},{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":null}}]}";
    private static final String EDITOR_DOC_TEXT =
            "Tiêu đề “có ngoặc” \\ và \"kép\"\n\n  Dòng thụt đầu, cuối có cách  \n-------------page_break------------------\nTrang hai — ký tự đặc biệt… №\n\n-------------page_break------------------\n";

    @Spy
    private ChapterContentParser chapterContentParser = new JacksonChapterContentParser();

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

    @Test
    void update_contentJson_derivesRawTextEvenWhenAClientSendsADifferentOne() {
        when(chapterRepository.findById(12L)).thenReturn(Optional.of(chapter));

        chapterService.update(12L, ChapterUpdateRequest.builder()
                .contentJson(EDITOR_DOC).rawText("Không khớp").build());

        assertEquals(EDITOR_DOC, chapter.getContentJson());
        assertEquals(EDITOR_DOC_TEXT, chapter.getRawText());
        verify(chapterRepository).update(chapter);
    }

    @Test
    void update_rawTextAlone_clearsFormattingThatNoLongerMatches() {
        chapter.setContentJson(EDITOR_DOC);
        when(chapterRepository.findById(12L)).thenReturn(Optional.of(chapter));

        chapterService.update(12L, ChapterUpdateRequest.builder().rawText("Mới").build());

        assertEquals("Mới", chapter.getRawText());
        assertNull(chapter.getContentJson());
    }

    @Test
    void update_titleAlone_keepsTextAndFormatting() {
        chapter.setContentJson(EDITOR_DOC);
        when(chapterRepository.findById(12L)).thenReturn(Optional.of(chapter));

        chapterService.update(12L, ChapterUpdateRequest.builder().title("Tiêu đề").build());

        assertEquals("Nội dung", chapter.getRawText());
        assertEquals(EDITOR_DOC, chapter.getContentJson());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{not json", "[]", "{\"type\":\"doc\"}", "{\"content\":[{\"type\":\"heading\"}]}", "{\"content\":[{}]}"})
    void update_invalidContentJson_isRejectedWithoutSaving(String contentJson) {
        when(chapterRepository.findById(12L)).thenReturn(Optional.of(chapter));
        ChapterUpdateRequest req = ChapterUpdateRequest.builder().contentJson(contentJson).build();

        BusinessException ex = assertThrows(BusinessException.class, () -> chapterService.update(12L, req));

        assertEquals(BusinessError.CHAPTER_CONTENT_INVALID, ex.getError());
        assertEquals("Nội dung", chapter.getRawText());
        verify(chapterRepository, never()).update(any());
    }

    private GenerateAudioRequest captureJob() {
        ArgumentCaptor<GenerateAudioRequest> captor = ArgumentCaptor.forClass(GenerateAudioRequest.class);
        verify(messageQueue).send(captor.capture());
        return captor.getValue();
    }
}
