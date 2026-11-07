package com.backend.application.adapter.repository;

import com.backend.domain.model.BookFavorite;
import java.util.List;


public interface BookFavoriteRepository {

    BookFavorite save(BookFavorite bookFavorite);

    boolean existsByCustomerIdAndBookId(Long customerId, Long bookId);

    void deleteByCustomerIdAndBookId(Long customerId, Long bookId);

    List<Long> findBookIdsByCustomerId(Long customerId);
}
