package com.backend.infrastructure.persistence.entity;

import com.backend.domain.valueobject.ChapterStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tbl_chapter")
@Getter
@Setter
public class JpaChapterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "chapter_order", nullable = false)
    private Integer chapterOrder;

    private Integer duration;

    @Column(name = "audio_url")
    private String audioUrl;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private ChapterStatus status;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "reading_speed")
    private Double readingSpeed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", insertable = false, updatable = false)
    private JpaBookEntity book;
}
