package com.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tbl_customer_book")
@Getter
@Setter
public class JpaCustomerBookEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "customer_id", nullable = false)
  private Long customerId;

  @Column(name = "book_id", nullable = false)
  private Long bookId;

  @Column(name = "purchase_at", nullable = false)
  private Instant purchaseAt;

  @Column(name = "is_read_complete", nullable = false)
  private Boolean isReadComplete;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id", insertable = false, updatable = false)
  private JpaCustomerEntity customer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "book_id", insertable = false, updatable = false)
  private JpaBookEntity book;

  @PrePersist
  void onCreate() {
    if (purchaseAt == null) purchaseAt = Instant.now();
  }
}
