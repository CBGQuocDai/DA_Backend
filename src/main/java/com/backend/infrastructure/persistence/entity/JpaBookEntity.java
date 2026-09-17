package com.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "books")
@Getter
@Setter
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
}
