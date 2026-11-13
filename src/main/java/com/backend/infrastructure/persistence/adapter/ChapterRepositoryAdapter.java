package com.backend.infrastructure.persistence.adapter;

import com.backend.application.adapter.repository.ChapterRepository;
import com.backend.domain.model.Chapter;
import com.backend.infrastructure.persistence.mapper.ChapterMapper;
import com.backend.infrastructure.persistence.repository.JpaChapterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class ChapterRepositoryAdapter implements ChapterRepository {
    private final JpaChapterRepository jpaChapterRepository;
    private final ChapterMapper chapterMapper;

    @Override
    @Transactional(readOnly = true)
    public List<Chapter> findByBookId(Long bookId) {
        return jpaChapterRepository.findByBookId(bookId).stream()
                .map(chapterMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public Optional<Chapter> findById(Long id) {
        return jpaChapterRepository.findById(id).map(chapterMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Integer> findMaxChapterOrder(Long bookId) {
        return jpaChapterRepository.findMaxChapterOrder(bookId);
    }

    @Override
    @Transactional
    public void shiftOrders(Long bookId, int from, int to, int delta) {
        jpaChapterRepository.shiftOrders(bookId, from, to, delta);
    }

    @Override
    @Transactional
    public Chapter save(Chapter chapter) {
        return chapterMapper.toDomain(jpaChapterRepository.save(chapterMapper.toEntity(chapter)));
    }

    @Override
    @Transactional
    public void saveAll(List<Chapter> chapters) {
        jpaChapterRepository.saveAll(chapters.stream().map(chapterMapper::toEntity).toList())
                .stream().map(chapterMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public Chapter update(Chapter chapter) {
        jpaChapterRepository.updateChapter(chapter.getId(), chapter.getTitle(), chapter.getChapterOrder(),
                chapter.getDuration(), chapter.getAudioUrl(), chapter.getRawText(), chapter.getContentJson(),
                chapter.getStatus(), chapter.getAttachFile() == null ? null : chapter.getAttachFile().getId(),
                chapter.getReadingSpeed());
        return chapter;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpaChapterRepository.deleteById(id);
    }
}
