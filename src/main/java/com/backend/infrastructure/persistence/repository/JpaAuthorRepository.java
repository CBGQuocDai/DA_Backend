package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaAuthorEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaAuthorRepository extends JpaRepository<JpaAuthorEntity, Long> {

    /** {@code pattern} is already lower-cased, escaped and wrapped in '%'. */
    @Query("SELECT a FROM JpaAuthorEntity a WHERE LOWER(a.fullName) LIKE :pattern ESCAPE '\\' ORDER BY a.id DESC")
    Page<JpaAuthorEntity> search(@Param("pattern") String pattern, Pageable pageable);
}
