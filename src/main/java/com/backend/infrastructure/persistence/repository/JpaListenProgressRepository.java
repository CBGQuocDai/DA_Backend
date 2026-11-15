package com.backend.infrastructure.persistence.repository;

import com.backend.infrastructure.persistence.entity.JpaListenProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaListenProgressRepository extends JpaRepository<JpaListenProgressEntity, Long> {
    // Tìm danh sách tiến độ nghe theo khách hàng và sách
    List<JpaListenProgressEntity> findByCustomerIdAndBookId(Long customerId, Long bookId);

    // Tìm tiến độ nghe theo khách hàng và chương
    Optional<JpaListenProgressEntity> findByCustomerIdAndChapterId(Long customerId, Long chapterId);

    // Xóa toàn bộ tiến độ nghe của các chương thuộc sách khi xóa sách
    void deleteByBookId(Long bookId);
}
