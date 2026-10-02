package com.backend.application.impl;

import com.backend.application.ChapterService;
import com.backend.domain.adapter.repository.BookRepository;
import com.backend.domain.adapter.repository.ChapterRepository;
import com.backend.domain.adapter.storage.StorageService;
import com.backend.domain.adapter.textToSpeech.TextToSpeechService;
import com.backend.domain.dto.request.book.chapter.GenerateAudioRequest;
import com.backend.domain.model.Book;
import com.backend.domain.model.Chapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChapterServiceImpl implements ChapterService {

    private final ChapterRepository chapterRepository;
    private final TextToSpeechService  textToSpeechService;
    private final BookRepository bookRepository;
    private final StorageService storageService;

    @Override
    @Transactional(readOnly = true)
    public List<Chapter> getChaptersByBookId(Long bookId) {
        List<Chapter> chapters = chapterRepository.findByBookId(bookId);
        return chapters.stream().peek(c -> c.setAudioUrl(storageService.createSignUrl(c.getAudioUrl(),60L))).toList();
    }

    @Override
    @Transactional
    public void deleteChapter(Long chapterId) {
        chapterRepository.delete(chapterId);
    }

    @Override
    public void generateAudio(Long bookId, Long chapterId) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if(Objects.isNull(book)) {
            throw new RuntimeException("Book not found");
        }
        Chapter chapter = chapterRepository.findById(chapterId).orElse(null);
        if(Objects.isNull(chapter)) {
            throw new RuntimeException("Chapter not found");
        }
        log.info("Generating audio for book {} with voiceId {} and chapter {}", bookId, book.getVoice() != null ? book.getVoice().getId() : null, chapterId);
        GenerateAudioRequest request = GenerateAudioRequest.builder()
                .exampleVoiceUrl(book.getVoice() != null ? book.getVoice().getExampleAudio() : "")
                .text(chapter.getRawText())
                .build();
        String path = textToSpeechService.generateAudio(request);
        chapter.setAudioUrl(path);
        chapterRepository.update(chapter);
    }
}
