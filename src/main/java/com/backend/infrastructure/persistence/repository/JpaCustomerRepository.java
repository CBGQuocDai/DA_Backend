package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaCustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@Repository
public interface JpaCustomerRepository extends JpaRepository<JpaCustomerEntity, Long> {

    Optional<JpaCustomerEntity> findByEmail(String email);

    // (Pessimistic Lock) chống xung đột cập nhật đồng thời
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM JpaCustomerEntity c WHERE c.id = :id")
    Optional<JpaCustomerEntity> findByIdForUpdate(@Param("id") Long id);
}
