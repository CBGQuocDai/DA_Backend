package com.backend.presentation.controller;

import com.backend.application.service.BookFavoriteService;
import com.backend.presentation.dto.ApiResponse;
import com.backend.application.dto.response.BookFavoriteResponse;
import com.backend.application.dto.response.book.BookResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PostMapping("/{bookId}/toggle")
    public ApiResponse<BookFavoriteResponse> toggleFavorite(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long bookId) {
        BookFavoriteResponse data = bookFavoriteService.toggleFavorite(userId, bookId);
        String message = Boolean.TRUE.equals(data.getIsFavorite())
            ? "Đã thêm vào danh sách yêu thích"
            : "Đã xóa khỏi danh sách yêu thích";
        return ApiResponse.<BookFavoriteResponse>builder()
            .message(message)
            .data(data)
            .build();
    }

    @GetMapping("/{bookId}/check")
    public ApiResponse<Boolean> checkFavorite(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long bookId) {
        boolean isFav = bookFavoriteService.isFavorite(userId, bookId);
        return ApiResponse.<Boolean>builder()
            .data(isFav)
            .build();
    }

    @GetMapping
    public ApiResponse<List<BookResponse>> list(@AuthenticationPrincipal Long userId) {
        List<BookResponse> data = bookFavoriteService.list(userId);
        return ApiResponse.<List<BookResponse>>builder()
            .data(data)
            .build();
    }
}
