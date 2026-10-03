package com.backend.presentation.controller;

import com.backend.application.BookFavoriteService;
import com.backend.domain.dto.response.ApiResponse;
import com.backend.domain.dto.response.BookFavoriteResponse;
import com.backend.domain.model.Book;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/book-favorite")
@RequiredArgsConstructor
public class BookFavoriteController {

    private final BookFavoriteService bookFavoriteService;

    @PostMapping("/{bookId}")
    public ApiResponse<BookFavoriteResponse> addFavorite(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long bookId) {
        BookFavoriteResponse data = bookFavoriteService.addFavorite(userId, bookId);
        return ApiResponse.<BookFavoriteResponse>builder()
            .message("Đã thêm vào danh sách yêu thích")
            .data(data)
            .build();
    }

    @DeleteMapping("/{bookId}")
    public ApiResponse<Void> removeFavorite(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long bookId) {
        bookFavoriteService.removeFavorite(userId, bookId);
        return ApiResponse.<Void>builder()
            .message("Đã xóa khỏi danh sách yêu thích")
            .build();
    }

    @GetMapping
    public ApiResponse<List<Book>> list(@AuthenticationPrincipal Long userId) {
        List<Book> data = bookFavoriteService.list(userId);
        return ApiResponse.<List<Book>>builder()
            .data(data)
            .build();
    }
}
