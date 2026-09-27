package com.backend.presentation.controller;

import com.backend.application.BookFavoriteService;
import com.backend.domain.dto.request.user.RegisterRequest;
import com.backend.domain.dto.response.ApiResponse;
import com.backend.domain.dto.response.BookFavoriteResponse;
import com.backend.domain.dto.response.user.RegisterResponse;
import com.backend.domain.model.BookFavorite;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/book-favorite")
@RequiredArgsConstructor
public class BookFavoriteController {

    private final BookFavoriteService bookFavoriteService;

    @PostMapping("/{bookId}")
    public ApiResponse<BookFavoriteResponse> addFavorite(@AuthenticationPrincipal Long userId,
        @PathVariable Long bookId) {
        BookFavoriteResponse data = bookFavoriteService.addFavorite(userId, bookId);
        return ApiResponse.<BookFavoriteResponse>builder()
            .message("đã thêm và danh sách yêu thích")
            .data(data).build();
    }

}
