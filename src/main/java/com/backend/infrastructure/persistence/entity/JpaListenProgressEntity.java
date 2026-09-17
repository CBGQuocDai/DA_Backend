package com.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "listen_progress")
@Getter
@Setter
public class JpaListenProgressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "is_complete", nullable = false)
    private Boolean isComplete;

    @Column(name = "last_play_at", nullable = false)
    private Integer lastPlayAt;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "chapter_id")
    private Long chapterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private JpaCustomerEntity customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", insertable = false, updatable = false)
    private JpaBookEntity book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", insertable = false, updatable = false)
    private JpaChapterEntity chapter;
}
