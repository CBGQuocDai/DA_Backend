package com.backend.presentation.controller;

import com.backend.application.service.ReviewService;
import com.backend.domain.dto.request.review.ReviewCreateRequest;
import com.backend.domain.dto.response.ApiResponse;
import com.backend.domain.dto.response.review.ReviewCreateResponse;
import com.backend.domain.dto.response.review.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ApiResponse<ReviewCreateResponse> save(
        @AuthenticationPrincipal Long userId,
        @Validated @RequestBody ReviewCreateRequest request) {
        ReviewCreateResponse data = reviewService.save(userId, request);
        return ApiResponse.<ReviewCreateResponse>builder()
            .message("Tạo đánh giá thành công")
            .data(data).build();
    }

    @GetMapping("/{bookId}")
    public ApiResponse<List<ReviewResponse>> getReviewByBookId(
        @PathVariable Long bookId
    ) {
        List<ReviewResponse> data = reviewService.getReviewByBookId(bookId);
        return ApiResponse.<List<ReviewResponse>>builder()
            .message("Lấy danh sách đánh giá thành công")
            .data(data).build();
    }
}
