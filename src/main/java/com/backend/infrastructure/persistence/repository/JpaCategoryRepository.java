package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaCategoryEntity;
import com.backend.infrastructure.persistence.projection.CategorySalesProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

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

    /**
     * Categories whose books were bought in [from, to), with their purchases in [since, from) beside them;
     * a book in several categories counts in each.
     */
    @Query("""
            SELECT c.id AS id, c.name AS name,
                   SUM(CASE WHEN cb.purchaseAt >= :from THEN 1 ELSE 0 END) AS purchases,
                   SUM(CASE WHEN cb.purchaseAt < :from THEN 1 ELSE 0 END) AS previousPurchases
            FROM JpaBookCategoryEntity bc
            JOIN JpaCustomerBookEntity cb ON cb.bookId = bc.bookId
            JOIN JpaCategoryEntity c ON c.id = bc.categoryId
            WHERE cb.purchaseAt >= :since AND cb.purchaseAt < :to
            GROUP BY c.id, c.name
            HAVING SUM(CASE WHEN cb.purchaseAt >= :from THEN 1 ELSE 0 END) > 0
            ORDER BY SUM(CASE WHEN cb.purchaseAt >= :from THEN 1 ELSE 0 END) DESC, c.name
            """)
    List<CategorySalesProjection> findBestsellers(@Param("since") Instant since, @Param("from") Instant from,
                                                  @Param("to") Instant to);
}
