package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaAttachFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaAttachFileRepository extends JpaRepository<JpaAttachFileEntity, Long> {
}
