package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaChapterEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import com.backend.domain.valueobject.ChapterStatus;
import com.backend.domain.valueobject.PlaybackChapter;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaChapterRepository extends JpaRepository<JpaChapterEntity, Long> {

    // Lấy danh sách chương rút gọn theo sách để phát audio
    @Query("""
        SELECT new com.backend.domain.valueobject.PlaybackChapter(
            c.id, c.title, c.chapterOrder, c.duration, c.audioUrl, c.status)
        FROM JpaChapterEntity c WHERE c.bookId = :bookId ORDER BY c.chapterOrder ASC, c.id ASC
        """)
    List<PlaybackChapter> findPlaybackChaptersByBookId(@Param("bookId") Long bookId);

    /**
     * Lấy chương của sách theo thứ tự chương tăng dần.
     */
    @Query("""
            SELECT c FROM JpaChapterEntity c
            WHERE c.bookId = :bookId
            ORDER BY c.chapterOrder ASC
            """)
    List<JpaChapterEntity> findByBookId(@Param("bookId") Long bookId);

    /**
     * Lấy chương theo ID và giữ khóa ghi đến hết giao dịch.
     */
    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM JpaChapterEntity c WHERE c.id = :id")
    Optional<JpaChapterEntity> findById(@Param("id") Long id);

    /** Lấy tài liệu của chương mà không khóa dòng. */
    @Query("SELECT c.content FROM JpaChapterEntity c WHERE c.id = :id")
    Optional<String> findContentById(@Param("id") Long id);

    /**
     * Lấy thứ tự chương lớn nhất của sách.
     */
    @Query("SELECT MAX(c.chapterOrder) FROM JpaChapterEntity c WHERE c.bookId = :bookId")
    Optional<Integer> findMaxChapterOrder(@Param("bookId") Long bookId);

    /**
     * Xóa toàn bộ chương thuộc sách.
     */
    void deleteByBookId(Long bookId);

    /**
     * Cập nhật dữ liệu chương theo các trường của domain entity.
     */
    @Modifying
    @Query("UPDATE JpaChapterEntity c SET " +
        "c.title = :title, " +
        "c.chapterOrder = :chapterOrder, " +
        "c.duration = :duration, " +
        "c.audioUrl = :audioUrl, " +
        "c.content = :content, " +
        "c.status = :status, " +
        "c.readingSpeed = :readingSpeed " +
        "WHERE c.id = :id")
    void updateChapter(
            @Param("id") Long id,
            @Param("title") String title,
            @Param("chapterOrder") Integer chapterOrder,
            @Param("duration") Integer duration,
            @Param("audioUrl") String audioUrl,
            @Param("content") String content,
            @Param("status") ChapterStatus status,
            @Param("readingSpeed") Double readingSpeed
    );

    /** Dịch thứ tự chương trong đoạn chỉ định bằng một câu lệnh cập nhật. */
    @Modifying
    @Query("UPDATE JpaChapterEntity c SET c.chapterOrder = c.chapterOrder + :delta " +
            "WHERE c.bookId = :bookId AND c.chapterOrder BETWEEN :from AND :to")
    void shiftOrders(
            @Param("bookId") Long bookId,
            @Param("from") int from,
            @Param("to") int to,
            @Param("delta") int delta
    );

}
