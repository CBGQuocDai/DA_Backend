package com.backend.application.service;

import com.backend.application.dto.response.BookFavoriteResponse;
import com.backend.application.dto.response.book.BookResponse;
import java.util.List;

public interface BookFavoriteService {

    BookFavoriteResponse toggleFavorite(Long userId, Long bookId);

    boolean isFavorite(Long userId, Long bookId);

    List<BookResponse> list(Long userId);
}
