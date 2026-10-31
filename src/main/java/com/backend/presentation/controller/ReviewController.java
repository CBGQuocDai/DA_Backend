package com.backend.presentation.controller;

import com.backend.application.service.ReviewService;
import com.backend.application.dto.request.review.ReviewCreateRequest;
import com.backend.presentation.dto.ApiResponse;
import com.backend.application.dto.response.review.ReviewCreateResponse;
import com.backend.application.dto.response.review.ReviewResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
