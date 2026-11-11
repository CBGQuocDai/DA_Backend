package com.backend.presentation.controller;

import com.backend.application.dto.request.author.AuthorRequest;
import com.backend.application.dto.response.PageResponse;
import com.backend.application.dto.response.author.AuthorResponse;
import com.backend.application.service.AuthorService;
import com.backend.presentation.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/authors")
@RequiredArgsConstructor
public class AuthorController {

    private final AuthorService authorService;

////    bỏ
//    @GetMapping("/page")
//    public ResponseEntity<ApiResponse<PageResponse<AuthorResponse>>> getCustomerAuthors(
//            @RequestParam(required = false) String keyword,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "8") int size) {
//        return ResponseEntity.ok(ApiResponse.<PageResponse<AuthorResponse>>builder()
//                .data(authorService.getCustomerAuthors(keyword, page, size)).build());
//    }

//  lấy thông tin tác giả
    @GetMapping("/{authorId}")
    public ResponseEntity<ApiResponse<AuthorResponse>> getAuthor(@PathVariable Long authorId) {
        return ResponseEntity.ok(ApiResponse.<AuthorResponse>builder()
                .data(authorService.getAuthor(authorId))
                .build());
    }

//    lấy danh sách cho dropdown
    @GetMapping
    public ResponseEntity<ApiResponse<List<AuthorResponse>>> getAuthors() {
        return ResponseEntity.ok(ApiResponse.<List<AuthorResponse>>builder()
                .data(authorService.getAuthors())
                .build());
    }

//  lấy thông tin
//    @PreAuthorize("hasRole('ADMIN')")
//    order theo tên luôn mặc định a -> z
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<AuthorResponse>>> searchAuthors(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<AuthorResponse>>builder()
                .data(authorService.searchAuthors(keyword, Math.max(page, 0), clampSize(size)))
                .build());
    }

//    tạo tác giả
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<AuthorResponse>> createAuthor(@Valid @RequestBody AuthorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<AuthorResponse>builder()
                .data(authorService.createAuthor(request))
                .build());
    }

//    cập nhật
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{authorId}")
    public ResponseEntity<ApiResponse<AuthorResponse>> updateAuthor(
            @PathVariable Long authorId,
            @Valid @RequestBody AuthorRequest request) {
        return ResponseEntity.ok(ApiResponse.<AuthorResponse>builder()
                .data(authorService.updateAuthor(authorId, request))
                .build());
    }

//    xóa tác giả
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{authorId}")
    public ResponseEntity<ApiResponse<Void>> deleteAuthor(@PathVariable Long authorId) {
        authorService.deleteAuthor(authorId);
        return ResponseEntity.ok(ApiResponse.<Void>builder().build());
    }

    private static int clampSize(int size) {
        return Math.clamp(size, 1, 100);
    }
}
