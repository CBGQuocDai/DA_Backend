package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaCategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaCategoryRepository extends JpaRepository<JpaCategoryEntity, Long> {

    /** {@code pattern} is already lower-cased, escaped and wrapped in '%'. */
    @Query("""
            SELECT c FROM JpaCategoryEntity c
            WHERE LOWER(c.name) LIKE :pattern ESCAPE '\\'
               OR LOWER(COALESCE(c.description, '')) LIKE :pattern ESCAPE '\\'
            ORDER BY c.id DESC
            """)
    Page<JpaCategoryEntity> search(@Param("pattern") String pattern, Pageable pageable);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
