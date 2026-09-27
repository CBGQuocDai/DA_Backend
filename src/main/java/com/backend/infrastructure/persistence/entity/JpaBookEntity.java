package com.backend.infrastructure.persistence.entity;

import com.backend.domain.valueobject.BookStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tbl_book")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JpaBookEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "is_publish", nullable = false)
    private Boolean isPublish;

    @Column(nullable = false)
    private BigDecimal price;

    private String description;

    @Column(name = "cover_image")
    private String coverImage;

    @Column(name = "voice_id")
    private Long voiceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voice_id", insertable = false, updatable = false)
    private JpaVoiceEntity voice;

    @Enumerated(EnumType.STRING)
    private BookStatus status;
}
