package com.backend.presentation.controller;

import com.backend.application.service.CategoryService;
import com.backend.presentation.dto.ApiResponse;
import com.backend.application.dto.response.category.CategoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.<List<CategoryResponse>>builder()
                .data(categoryService.getCategories())
                .build());
    }
}
