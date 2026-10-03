package com.backend.presentation.controller;

import com.backend.application.ReviewService;
import com.backend.domain.dto.request.review.ReviewCreateRequest;
import com.backend.domain.dto.response.ApiResponse;
import com.backend.domain.dto.response.review.ReviewCreateResponse;
import com.backend.domain.dto.response.user.RegisterResponse;
import com.backend.domain.model.Review;
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

    public final ReviewService reviewService;

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
    public ApiResponse<List<Review>> getReviewByBookId(
        @PathVariable Long bookId
    ) {
        List<Review> data = reviewService.getReviewByBookId(bookId);
        return ApiResponse.<List<Review>>builder()
            .message("Lấy danh sách đánh giá thành công")
            .data(data).build();
    }
}
