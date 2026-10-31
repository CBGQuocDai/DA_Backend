package com.backend.presentation.controller;

import com.backend.application.service.AuthorService;
import com.backend.presentation.dto.ApiResponse;
import com.backend.application.dto.response.author.AuthorResponse;
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
    public ResponseEntity<ApiResponse<List<AuthorResponse>>> getAuthors() {
        return ResponseEntity.ok(ApiResponse.<List<AuthorResponse>>builder()
                .data(authorService.getAuthors())
                .build());
    }
}
