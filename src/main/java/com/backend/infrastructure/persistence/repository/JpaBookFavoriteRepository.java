package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaBookFavoriteEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaBookFavoriteRepository extends JpaRepository<JpaBookFavoriteEntity, Long> {

    List<JpaBookFavoriteEntity> findByCustomerId(Long customerId);

    List<JpaBookFavoriteEntity> findByBookId(Long bookId);

    Optional<JpaBookFavoriteEntity> findByCustomerIdAndBookId(Long customerId, Long bookId);

    boolean existsByCustomerIdAndBookId(Long customerId, Long bookId);

    void deleteByCustomerIdAndBookId(Long customerId, Long bookId);
}
