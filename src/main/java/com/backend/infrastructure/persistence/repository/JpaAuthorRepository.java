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

    /** Tìm tên không phân biệt dấu và hoa thường; sắp xếp tên A–Z, rồi ID để ổn định phân trang. */
    @Query(value = """
            SELECT a.*
            FROM tbl_author a
            WHERE TRANSLATE(LOWER(a.full_name), :accented, :plain) LIKE :keyword ESCAPE '\\'
            ORDER BY TRANSLATE(LOWER(a.full_name), :accented, :plain) ASC, a.full_name ASC, a.id ASC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM tbl_author a
            WHERE TRANSLATE(LOWER(a.full_name), :accented, :plain) LIKE :keyword ESCAPE '\\'
            """,
            nativeQuery = true)
    Page<JpaAuthorEntity> searchByName(
            @Param("keyword") String keyword,
            @Param("accented") String accented,
            @Param("plain") String plain,
            Pageable pageable);
}
