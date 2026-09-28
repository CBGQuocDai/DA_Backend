package com.backend.application;

import com.backend.domain.dto.response.BookFavoriteResponse;
import com.backend.domain.model.Book;
import java.util.List;

public interface BookFavoriteService {

    BookFavoriteResponse addFavorite(Long userId, Long bookId);

    void removeFavorite(Long userId, Long bookId);

    List<Book> list(Long userId);
}
