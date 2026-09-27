package com.backend.infrastructure.persistence.repository;

import com.backend.domain.valueobject.BookStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface JpaBookUpdateRepository extends JpaRepository<com.backend.infrastructure.persistence.entity.JpaBookEntity, Long> {

    @Modifying
    @Query("UPDATE JpaBookEntity b SET " +
            "b.title = :title, " +
            "b.price = :price, " +
            "b.description = :description, " +
            "b.coverImage = :coverImage, " +
            "b.contentFile = :contentFile, " +
            "b.voiceId = :voiceId, " +
            "b.status = :status " +
            "WHERE b.id = :id")
    void updateBook(
            @Param("id") Long id,
            @Param("title") String title,
            @Param("price") BigDecimal price,
            @Param("description") String description,
            @Param("coverImage") String coverImage,
            @Param("contentFile") String contentFile,
            @Param("voiceId") Long voiceId,
            @Param("status") BookStatus status
    );
}
