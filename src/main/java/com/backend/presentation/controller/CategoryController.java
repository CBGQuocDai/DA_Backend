package com.backend.presentation.controller;

import com.backend.application.dto.request.category.CategoryRequest;
import com.backend.application.dto.response.PageResponse;
import com.backend.application.dto.response.category.CategoryResponse;
import com.backend.application.service.CategoryService;
import com.backend.domain.valueobject.BestsellerPeriod;
import com.backend.presentation.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /** Every category, unpaged: for the store tabs and pickers such as the book form. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.<List<CategoryResponse>>builder()
                .data(categoryService.getCategories())
                .build());
    }

    /** Admin list: {@code keyword} filters by name or description (contains, case-insensitive), newest first. */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> searchCategories(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<CategoryResponse>>builder()
                .data(categoryService.searchCategories(keyword, Math.max(page, 0), clampSize(size)))
                .build());
    }

    /** Admin dashboard: categories by purchases in the period; a book in several categories counts in each. */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/bestsellers")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getBestsellers(
            @RequestParam(defaultValue = "MONTH") BestsellerPeriod period) {
        return ResponseEntity.ok(ApiResponse.<List<CategoryResponse>>builder()
                .data(categoryService.getBestsellers(period))
                .build());
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(ApiResponse.<CategoryResponse>builder()
                .data(categoryService.getCategory(categoryId))
                .build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<CategoryResponse>builder()
                .data(categoryService.createCategory(request))
                .build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.<CategoryResponse>builder()
                .data(categoryService.updateCategory(categoryId, request))
                .build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.<Void>builder().build());
    }

    private static int clampSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }
}
