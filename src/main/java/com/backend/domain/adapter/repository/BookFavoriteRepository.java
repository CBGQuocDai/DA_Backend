package com.backend.domain.adapter.repository;

import com.backend.domain.model.BookFavorite;
import java.util.List;


public interface BookFavoriteRepository {

    BookFavorite save(BookFavorite bookFavorite);

    void deleteById(Long bookFavoriteId);

    List<BookFavorite> findByCustomerId(Long customerId);

    boolean existsByCustomerIdAndBookId(Long customerId, Long bookId);

    void deleteByCustomerIdAndBookId(Long customerId, Long bookId);

    List<Long> findBookIdsByCustomerId(Long customerId);
}
