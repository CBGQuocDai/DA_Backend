package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaVoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaVoiceRepository extends JpaRepository<JpaVoiceEntity, Long> {
}
