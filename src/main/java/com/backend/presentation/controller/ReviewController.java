package com.backend.presentation.controller;

import com.backend.application.service.ReviewService;
import com.backend.application.dto.request.review.ReviewCreateRequest;
import com.backend.presentation.dto.ApiResponse;
import com.backend.application.dto.response.PageResponse;
import com.backend.application.dto.response.review.MyReviewResponse;
import com.backend.application.dto.response.review.ReviewSummaryResponse;
import com.backend.application.dto.response.review.ReviewResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping
    public ApiResponse<ReviewResponse> save(
        @AuthenticationPrincipal Long userId,
        @Validated @RequestBody ReviewCreateRequest request) {
        ReviewResponse data = reviewService.save(userId, request);
        return ApiResponse.<ReviewResponse>builder()
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

    @GetMapping("/{bookId}/page")
    public ApiResponse<PageResponse<ReviewResponse>> getPage(
            @PathVariable Long bookId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size) {
        return ApiResponse.<PageResponse<ReviewResponse>>builder()
                .data(reviewService.getPage(bookId, rating, sort, page, size)).build();
    }

    @GetMapping("/{bookId}/summary")
    public ApiResponse<ReviewSummaryResponse> getSummary(@PathVariable Long bookId) {
        return ApiResponse.<ReviewSummaryResponse>builder().data(reviewService.getSummary(bookId)).build();
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/{bookId}/me")
    public ApiResponse<MyReviewResponse> getMyReview(@PathVariable Long bookId,
                                                    @AuthenticationPrincipal Long customerId) {
        return ApiResponse.<MyReviewResponse>builder().data(reviewService.getMyReview(bookId, customerId)).build();
    }
}
