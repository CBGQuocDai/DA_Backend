package com.backend.infrastructure.persistence.adapter;

import com.backend.domain.adapter.repository.BookFavoriteRepository;
import com.backend.domain.mapper.BookFavoriteMapper;
import com.backend.domain.model.BookFavorite;
import com.backend.infrastructure.persistence.entity.JpaBookFavoriteEntity;
import com.backend.infrastructure.persistence.repository.JpaBookFavoriteRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BookFavoriteRepositoryAdapter implements BookFavoriteRepository {

    private final JpaBookFavoriteRepository jpaBookFavoriteRepository;
    private final BookFavoriteMapper bookFavoriteMapper;

    @Override
    public BookFavorite save(BookFavorite bookFavorite) {
        JpaBookFavoriteEntity saved = jpaBookFavoriteRepository
            .save(bookFavoriteMapper.toEntity(bookFavorite));
        return bookFavoriteMapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void deleteById(Long bookFavoriteId) {
        if (bookFavoriteId != null) {
            jpaBookFavoriteRepository.deleteById(bookFavoriteId);
        }
    }

    @Override
    public List<BookFavorite> findByCustomerId(Long customerId) {
        if (customerId == null) {
            return List.of();
        }
        return jpaBookFavoriteRepository.findByCustomerId(customerId).stream()
            .map(bookFavoriteMapper::toDomain)
            .toList();
    }

    @Override
    public boolean existsByCustomerIdAndBookId(Long customerId, Long bookId) {
        if (customerId == null || bookId == null) {
            return false;
        }
        return jpaBookFavoriteRepository.existsByCustomerIdAndBookId(customerId, bookId);
    }

    @Override
    @Transactional
    public void deleteByCustomerIdAndBookId(Long customerId, Long bookId) {
        if (customerId == null || bookId == null) {
            return;
        }
        jpaBookFavoriteRepository.deleteByCustomerIdAndBookId(customerId, bookId);
    }

    @Override
    public List<Long> findBookIdsByCustomerId(Long customerId) {
        if (customerId == null) {
            return List.of();
        }
        return jpaBookFavoriteRepository.findByCustomerId(customerId).stream()
            .map(JpaBookFavoriteEntity::getBookId)
            .distinct()
            .toList();
    }
}
