package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaUserRepository extends JpaRepository<JpaUserEntity, Long> {
    boolean existsByEmail(String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE JpaUserEntity u SET u.password = :password WHERE u.id = :id")
    void updatePassword(@Param("id") Long id, @Param("password") String password);
}
