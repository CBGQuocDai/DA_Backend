package com.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "tbl_review")
@Getter
@Setter
public class JpaReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double rate;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "create_at", nullable = false, updatable = false)
    private Instant createAt;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private JpaCustomerEntity customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", insertable = false, updatable = false)
    private JpaBookEntity book;

    @PrePersist
    void onCreate() {
        if (createAt == null) createAt = Instant.now();
    }
}
