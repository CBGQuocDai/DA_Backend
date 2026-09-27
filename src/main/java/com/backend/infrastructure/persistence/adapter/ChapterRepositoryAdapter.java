package com.backend.infrastructure.persistence.adapter;

import com.backend.domain.adapter.repository.ChapterRepository;
import com.backend.domain.mapper.ChapterMapper;
import com.backend.domain.model.Chapter;
import com.backend.infrastructure.persistence.entity.JpaChapterEntity;
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
        return jpaChapterRepository.findByBookIdOrderByChapterOrderAsc(bookId)
                .stream()
                .map(chapterMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Chapter> findById(Long id) {
        return jpaChapterRepository.findById(id)
                .map(chapterMapper::toDomain);
    }

    @Override
    @Transactional
    public Chapter save(Chapter chapter) {
        JpaChapterEntity entity = chapterMapper.toEntity(chapter);
        JpaChapterEntity savedEntity = jpaChapterRepository.save(entity);
        chapter.setId(savedEntity.getId());
        return chapter;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        jpaChapterRepository.deleteById(id);
    }
}
