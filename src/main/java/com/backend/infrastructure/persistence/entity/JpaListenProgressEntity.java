package com.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

// Thực thể JPA lưu trữ tiến độ nghe từng chương của khách hàng
@Entity
@Table(name = "tbl_listen_progress", uniqueConstraints = @UniqueConstraint(
        name = "uk_listen_progress_customer_chapter", columnNames = {"customer_id", "chapter_id"}),
        indexes = @Index(name = "idx_listen_progress_customer_book", columnList = "customer_id, book_id"))
@Check(name = "ck_listen_progress_position", constraints = "last_play_at >= 0")
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

    @Column(name = "updated_at", nullable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private Instant updatedAt;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "chapter_id", nullable = false)
    private Long chapterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private JpaCustomerEntity customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "book_id", insertable = false, updatable = false)
    private JpaBookEntity book;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "chapter_id", insertable = false, updatable = false)
    private JpaChapterEntity chapter;
}
