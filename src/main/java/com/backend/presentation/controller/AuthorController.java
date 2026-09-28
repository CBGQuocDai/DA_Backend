package com.backend.presentation.controller;

import com.backend.application.AuthorService;
import com.backend.domain.dto.response.ApiResponse;
import com.backend.domain.model.Author;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/authors")
@RequiredArgsConstructor
public class AuthorController {

    private final AuthorService authorService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Author>>> getAuthors() {
        return ResponseEntity.ok(ApiResponse.<List<Author>>builder()
                .data(authorService.getAuthors())
                .build());
    }
}
