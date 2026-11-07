package com.backend.presentation.controller;

import com.backend.application.dto.request.author.AuthorRequest;
import com.backend.application.dto.response.PageResponse;
import com.backend.application.dto.response.author.AuthorResponse;
import com.backend.application.service.AuthorService;
import com.backend.presentation.dto.ApiResponse;
import jakarta.validation.Valid;
import com.backend.application.dto.response.author.AuthorResponse;
import com.backend.application.dto.response.author.CustomerAuthorResponse;
import com.backend.application.dto.response.PageResponse;
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

    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<CustomerAuthorResponse>>> getCustomerAuthors(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "name_asc") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<CustomerAuthorResponse>>builder()
                .data(authorService.getCustomerAuthors(keyword, sort, page, size)).build());
    }

    @GetMapping("/{authorId}")
    public ResponseEntity<ApiResponse<CustomerAuthorResponse>> getCustomerAuthorById(@PathVariable Long authorId) {
        return ResponseEntity.ok(ApiResponse.<CustomerAuthorResponse>builder()
                .data(authorService.getCustomerAuthorById(authorId)).build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AuthorResponse>>> getAuthors() {
        return ResponseEntity.ok(ApiResponse.<List<AuthorResponse>>builder()
                .data(authorService.getAuthors())
                .build());
    }

    /** Admin list: {@code keyword} filters by name (contains, case-insensitive), newest first. */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<AuthorResponse>>> searchAuthors(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<AuthorResponse>>builder()
                .data(authorService.searchAuthors(keyword, Math.max(page, 0), clampSize(size)))
                .build());
    }

    @GetMapping("/{authorId}")
    public ResponseEntity<ApiResponse<AuthorResponse>> getAuthor(@PathVariable Long authorId) {
        return ResponseEntity.ok(ApiResponse.<AuthorResponse>builder()
                .data(authorService.getAuthor(authorId))
                .build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<AuthorResponse>> createAuthor(@Valid @RequestBody AuthorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<AuthorResponse>builder()
                .data(authorService.createAuthor(request))
                .build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{authorId}")
    public ResponseEntity<ApiResponse<AuthorResponse>> updateAuthor(
            @PathVariable Long authorId,
            @Valid @RequestBody AuthorRequest request) {
        return ResponseEntity.ok(ApiResponse.<AuthorResponse>builder()
                .data(authorService.updateAuthor(authorId, request))
                .build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{authorId}")
    public ResponseEntity<ApiResponse<Void>> deleteAuthor(@PathVariable Long authorId) {
        authorService.deleteAuthor(authorId);
        return ResponseEntity.ok(ApiResponse.<Void>builder().build());
    }

    private static int clampSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }
}
