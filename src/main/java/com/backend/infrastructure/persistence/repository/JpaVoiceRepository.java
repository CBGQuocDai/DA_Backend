package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaVoiceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaVoiceRepository extends JpaRepository<JpaVoiceEntity, Long> {

    @Modifying
    @Query("UPDATE JpaVoiceEntity v SET v.name = :name, v.description = :description, v.exampleAudio = :exampleAudio WHERE v.id = :id")
    int updateVoice(@Param("id") Long id,
                    @Param("name") String name,
                    @Param("description") String description,
                    @Param("exampleAudio") String exampleAudio);

    @Query("SELECT v FROM JpaVoiceEntity v WHERE LOWER(v.name) LIKE :voiceName ESCAPE '\\'")
    Page<JpaVoiceEntity> findAllByNameLike(String voiceName, PageRequest pageRequest);
}
