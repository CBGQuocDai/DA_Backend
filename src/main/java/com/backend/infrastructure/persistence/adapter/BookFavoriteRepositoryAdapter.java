package com.backend.infrastructure.persistence.adapter;

import com.backend.domain.adapter.repository.BookFavoriteRepository;
import com.backend.domain.mapper.AdminMapper;
import com.backend.domain.mapper.BookFavoriteMapper;
import com.backend.domain.model.Admin;
import com.backend.domain.model.BookFavorite;
import com.backend.domain.model.Customer;
import com.backend.infrastructure.persistence.entity.JpaAdminEntity;
import com.backend.infrastructure.persistence.entity.JpaBookFavoriteEntity;
import com.backend.infrastructure.persistence.entity.JpaCustomerEntity;
import com.backend.infrastructure.persistence.repository.JpaAdminRepository;
import com.backend.infrastructure.persistence.repository.JpaBookFavoriteRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
    public void deleteById(Long bookFavoriteId) {
    }

    @Override
    public List<BookFavorite> findByCustomerId(Long customerId) {
        return List.of();
    }

    
    @Override
    public boolean existsByCustomerIdAndBookId(Long customerId, Long bookId) {
        return jpaBookFavoriteRepository.existsByCustomerIdAndBookId(customerId, bookId);
    }

    @Override
    public void deleteByCustomerIdAndBookId(Long customerId, Long bookId) {
        jpaBookFavoriteRepository.findByCustomerIdAndBookId(customerId, bookId)
            .ifPresent(jpaBookFavoriteRepository::delete);
    }

    @Override
    public List<Long> findBookIdsByCustomerId(Long customerId) {
        return jpaBookFavoriteRepository.findByCustomerId(customerId).stream()
            .map(JpaBookFavoriteEntity::getBookId)
            .toList();
    }
}
