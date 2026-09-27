package com.backend.application.impl;

import com.backend.application.ChapterService;
import com.backend.domain.adapter.repository.ChapterRepository;
import com.backend.domain.dto.request.book.ChapterMergeRequest;
import com.backend.domain.dto.request.book.ChapterSplitRequest;
import com.backend.domain.dto.request.book.ChapterUpdateRequest;
import com.backend.domain.model.Chapter;
import com.backend.domain.valueobject.ChapterStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChapterServiceImpl implements ChapterService {

    private final ChapterRepository chapterRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Chapter> getChaptersByBookId(Long bookId) {
        return chapterRepository.findByBookId(bookId);
    }

    @Override
    @Transactional(readOnly = true)
    public Chapter getChapter(Long bookId, Long chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new IllegalArgumentException("Chapter not found with id: " + chapterId));

        if (!chapter.getBook().getId().equals(bookId)) {
            throw new IllegalArgumentException("Chapter does not belong to the specified book");
        }

        return chapter;
    }

    @Override
    @Transactional
    public Chapter updateChapter(Long bookId, Long chapterId, ChapterUpdateRequest request) {
        Chapter chapter = getChapter(bookId, chapterId);

        if (request.getTitle() != null) {
            chapter.setTitle(request.getTitle());
        }
        if (request.getRawText() != null) {
            chapter.setRawText(request.getRawText());
        }
        if (request.getChapterOrder() != null) {
            reorderChapter(chapter, request.getChapterOrder());
        }

        return chapterRepository.save(chapter);
    }

    @Override
    @Transactional
    public Chapter splitChapter(Long bookId, Long chapterId, ChapterSplitRequest request) {
        Chapter originalChapter = getChapter(bookId, chapterId);

        if (originalChapter.getRawText() == null || originalChapter.getRawText().isEmpty()) {
            throw new IllegalArgumentException("Cannot split chapter with empty text");
        }

        int splitPosition = request.getSplitPosition();
        if (splitPosition <= 0 || splitPosition >= originalChapter.getRawText().length()) {
            throw new IllegalArgumentException("Invalid split position");
        }

        String firstPart = originalChapter.getRawText().substring(0, splitPosition);
        String secondPart = originalChapter.getRawText().substring(splitPosition);

        originalChapter.setRawText(firstPart);
        chapterRepository.save(originalChapter);

        List<Chapter> existingChapters = chapterRepository.findByBookId(bookId);
        int maxOrder = existingChapters.stream()
                .mapToInt(Chapter::getChapterOrder)
                .max()
                .orElse(0);

        Chapter newChapter = Chapter.builder()
                .title(originalChapter.getTitle() + " (Part 2)")
                .chapterOrder(maxOrder + 1)
                .rawText(secondPart)
                .status(ChapterStatus.TEXT_READY)
                .book(originalChapter.getBook())
                .build();

        return chapterRepository.save(newChapter);
    }

    @Override
    @Transactional
    public List<Chapter> mergeChapters(Long bookId, ChapterMergeRequest request) {
        List<Long> chapterIds = request.getChapterIds();

        if (chapterIds == null || chapterIds.size() < 2) {
            throw new IllegalArgumentException("At least two chapters required for merge");
        }

        List<Chapter> chaptersToMerge = new ArrayList<>();
        for (Long chapterId : chapterIds) {
            Chapter chapter = getChapter(bookId, chapterId);
            chaptersToMerge.add(chapter);
        }

        chaptersToMerge.sort((a, b) -> a.getChapterOrder().compareTo(b.getChapterOrder()));

        StringBuilder mergedText = new StringBuilder();
        for (Chapter chapter : chaptersToMerge) {
            if (chapter.getRawText() != null) {
                if (mergedText.length() > 0) {
                    mergedText.append("\n\n");
                }
                mergedText.append(chapter.getRawText());
            }
        }

        Chapter firstChapter = chaptersToMerge.get(0);
        String newTitle = request.getNewTitle() != null ? request.getNewTitle() : firstChapter.getTitle();
        firstChapter.setTitle(newTitle);
        firstChapter.setRawText(mergedText.toString());

        List<Chapter> chaptersToDelete = chaptersToMerge.subList(1, chaptersToMerge.size());
        for (Chapter chapter : chaptersToDelete) {
            chapterRepository.delete(chapter.getId());
        }

        Chapter savedChapter = chapterRepository.save(firstChapter);

        reindexChapters(bookId);

        return chapterRepository.findByBookId(bookId);
    }

    @Override
    @Transactional
    public void deleteChapter(Long bookId, Long chapterId) {
        Chapter chapter = getChapter(bookId, chapterId);
        chapterRepository.delete(chapterId);
        reindexChapters(bookId);
    }

    private void reorderChapter(Chapter chapter, int newOrder) {
        List<Chapter> chapters = chapterRepository.findByBookId(chapter.getBook().getId());
        int oldOrder = chapter.getChapterOrder();

        if (newOrder < oldOrder) {
            for (Chapter c : chapters) {
                if (c.getChapterOrder() >= newOrder && c.getChapterOrder() < oldOrder && !c.getId().equals(chapter.getId())) {
                    c.setChapterOrder(c.getChapterOrder() + 1);
                    chapterRepository.save(c);
                }
            }
        } else if (newOrder > oldOrder) {
            for (Chapter c : chapters) {
                if (c.getChapterOrder() > oldOrder && c.getChapterOrder() <= newOrder && !c.getId().equals(chapter.getId())) {
                    c.setChapterOrder(c.getChapterOrder() - 1);
                    chapterRepository.save(c);
                }
            }
        }

        chapter.setChapterOrder(newOrder);
    }

    private void reindexChapters(Long bookId) {
        List<Chapter> chapters = chapterRepository.findByBookId(bookId);
        chapters.sort((a, b) -> a.getChapterOrder().compareTo(b.getChapterOrder()));

        for (int i = 0; i < chapters.size(); i++) {
            Chapter chapter = chapters.get(i);
            if (!chapter.getChapterOrder().equals(i + 1)) {
                chapter.setChapterOrder(i + 1);
                chapterRepository.save(chapter);
            }
        }
    }
}
