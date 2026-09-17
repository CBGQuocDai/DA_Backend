package com.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "chapters")
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

    @Column(name = "text_context", columnDefinition = "TEXT")
    private String textContext;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "attach_file_id")
    private Long attachFileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", insertable = false, updatable = false)
    private JpaBookEntity book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attach_file_id", insertable = false, updatable = false)
    private JpaAttachFileEntity attachFile;
}
