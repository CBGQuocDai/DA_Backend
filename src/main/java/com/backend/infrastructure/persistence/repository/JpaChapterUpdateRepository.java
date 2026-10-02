package com.backend.infrastructure.persistence.repository;

import com.backend.domain.valueobject.ChapterStatus;
import com.backend.infrastructure.persistence.entity.JpaChapterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaChapterUpdateRepository extends JpaRepository<JpaChapterEntity, Long> {

    @Modifying
    @Query("UPDATE JpaChapterEntity c SET " +
            "c.title = :title, " +
            "c.chapterOrder = :chapterOrder, " +
            "c.duration = :duration, " +
            "c.audioUrl = :audioUrl, " +
            "c.rawText = :rawText, " +
            "c.status = :status, " +
            "c.attachFileId = :attachFileId " +
            "WHERE c.id = :id")
    void updateChapter(
            @Param("id") Long id,
            @Param("title") String title,
            @Param("chapterOrder") Integer chapterOrder,
            @Param("duration") Integer duration,
            @Param("audioUrl") String audioUrl,
            @Param("rawText") String rawText,
            @Param("status") ChapterStatus status,
            @Param("attachFileId") Long attachFileId
    );
}
