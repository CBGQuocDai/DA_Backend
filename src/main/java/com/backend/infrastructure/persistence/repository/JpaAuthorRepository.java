package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaAuthorEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.backend.infrastructure.persistence.projection.AuthorListProjection;
import com.backend.infrastructure.persistence.projection.AuthorSpecialtyProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaAuthorRepository extends JpaRepository<JpaAuthorEntity, Long> {

    /** {@code pattern} is already lower-cased, escaped and wrapped in '%'. */
    @Query("SELECT a FROM JpaAuthorEntity a WHERE LOWER(a.fullName) LIKE :pattern ESCAPE '\\' ORDER BY a.id DESC")
    Page<JpaAuthorEntity> search(@Param("pattern") String pattern, Pageable pageable);

    @Query(value = """
            SELECT a.id AS id,
                   a.full_name AS "fullName",
                   a.avatar AS avatar,
                   a.bio AS bio,
                   COUNT(DISTINCT b.id) AS "bookCount"
            FROM tbl_author a
            LEFT JOIN tbl_book_author ba ON ba.author_id = a.id
            LEFT JOIN tbl_book b ON b.id = ba.book_id AND b.status = 'PUBLISHED'
            WHERE TRANSLATE(LOWER(a.full_name), :accented, :plain) LIKE :keyword ESCAPE '\\'
            GROUP BY a.id, a.full_name, a.avatar, a.bio
            ORDER BY
                CASE WHEN :sort = 'name_asc'
                     THEN TRANSLATE(LOWER(a.full_name), :accented, :plain) END ASC,
                CASE WHEN :sort = 'name_asc' THEN a.full_name END ASC,
                CASE WHEN :sort = 'book_count_desc' THEN COUNT(DISTINCT b.id) END DESC,
                a.id ASC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM tbl_author a
            WHERE TRANSLATE(LOWER(a.full_name), :accented, :plain) LIKE :keyword ESCAPE '\\'
            """,
            nativeQuery = true)
    Page<AuthorListProjection> findCustomerAuthorProjections(
            @Param("keyword") String keyword,
            @Param("accented") String accented,
            @Param("plain") String plain,
            @Param("sort") String sort,
            Pageable pageable);

    @Query(value = """
            SELECT a.id AS id,
                   a.full_name AS "fullName",
                   a.avatar AS avatar,
                   a.bio AS bio,
                   COUNT(DISTINCT b.id) AS "bookCount"
            FROM tbl_author a
            LEFT JOIN tbl_book_author ba ON ba.author_id = a.id
            LEFT JOIN tbl_book b ON b.id = ba.book_id AND b.status = 'PUBLISHED'
            WHERE a.id = :authorId
            GROUP BY a.id, a.full_name, a.avatar, a.bio
            """, nativeQuery = true)
    Optional<AuthorListProjection> findCustomerAuthorProjectionById(@Param("authorId") Long authorId);

    @Query(value = """
            SELECT ba.author_id AS "authorId", c.name AS "categoryName"
            FROM tbl_book_author ba
            JOIN tbl_book b ON b.id = ba.book_id AND b.status = 'PUBLISHED'
            JOIN tbl_book_category bc ON bc.book_id = b.id
            JOIN tbl_category c ON c.id = bc.category_id
            WHERE ba.author_id IN (:authorIds)
            GROUP BY ba.author_id, c.name
            ORDER BY COUNT(DISTINCT b.id) DESC, c.name ASC
            """, nativeQuery = true)
    List<AuthorSpecialtyProjection> findSpecialtiesByAuthorIdIn(@Param("authorIds") List<Long> authorIds);
}
