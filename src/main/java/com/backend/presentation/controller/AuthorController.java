package com.backend.presentation.controller;

import com.backend.application.service.AuthorService;
import com.backend.presentation.dto.ApiResponse;
import com.backend.application.dto.response.author.AuthorResponse;
import com.backend.application.dto.response.author.CustomerAuthorResponse;
import com.backend.application.dto.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
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
}
