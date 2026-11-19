package com.backend.infrastructure.persistence.adapter;

import com.backend.application.adapter.repository.ListenProgressRepository;
import com.backend.domain.model.ListenProgress;
import com.backend.infrastructure.persistence.mapper.ListenProgressMapper;
import com.backend.infrastructure.persistence.repository.JpaListenProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ListenProgressRepositoryAdapter implements ListenProgressRepository {

    private final JpaListenProgressRepository repository;
    private final ListenProgressMapper mapper;

    // Lấy danh sách tiến độ nghe của khách hàng theo cuốn sách
    @Override
    public List<ListenProgress> findByCustomerIdAndBookId(Long customerId, Long bookId) {
        return repository.findByCustomerIdAndBookId(customerId, bookId)
            .stream()
            .map(mapper::toDomain)
            .toList();
    }

    // Lấy tiến độ nghe một chương cụ thể của khách hàng
    @Override
    public Optional<ListenProgress> findByCustomerIdAndChapterId(Long customerId, Long chapterId) {
        return repository.findByCustomerIdAndChapterId(customerId, chapterId)
            .map(mapper::toDomain);
    }

    // Lưu hoặc cập nhật bản ghi tiến độ nghe
    @Override
    public ListenProgress save(ListenProgress progress) {
        return mapper.toDomain(repository.save(mapper.toEntity(progress)));
    }
}
