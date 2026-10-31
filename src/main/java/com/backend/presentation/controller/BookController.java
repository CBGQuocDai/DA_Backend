package com.backend.presentation.controller;


import com.backend.application.service.BookService;
import com.backend.application.service.CategoryService;
import com.backend.application.dto.request.book.BookRequest;
import com.backend.presentation.dto.ApiResponse;
import com.backend.application.dto.response.PageResponse;
import com.backend.application.dto.response.book.BookResponse;
import com.backend.application.dto.response.category.CategoryResponse;
import com.backend.domain.valueobject.BookStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;
    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<PageResponse<BookResponse>> getListBooks(
            @RequestParam(required = false) String bookName,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) BookStatus status,
            @RequestParam Integer page,
            @RequestParam Integer size
    ) {
        return ResponseEntity.ok(bookService.getBooks(bookName, categoryId, authorId, status, page, size));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<PageResponse<BookResponse>> getBooksByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return ResponseEntity.ok(bookService.getBooksByCategory(categoryId, page, size));
    }

    @GetMapping("/{bookId}")
    public ResponseEntity<ApiResponse<BookResponse>> getBookById(@PathVariable Long bookId) {
        return ResponseEntity.ok(com.backend.presentation.dto.ApiResponse.<BookResponse>builder()
                .data(bookService.getBookById(bookId))
                .build());
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {
        return ResponseEntity.ok(com.backend.presentation.dto.ApiResponse.<List<CategoryResponse>>builder()
                .data(categoryService.getCategories())
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookResponse>> createBook(
            @Validated @RequestBody BookRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<BookResponse>builder()
                        .data(bookService.createBook(request))
                        .build()
                );
    }

    @PutMapping("/{bookId}")
    public ResponseEntity<ApiResponse<BookResponse>> updateBook(
            @PathVariable Long bookId,
            @Valid @RequestBody BookRequest request
    ) {
        return ResponseEntity.ok(com.backend.presentation.dto.ApiResponse.<BookResponse>builder()
                .data(bookService.updateBook(bookId, request))
                .build());
    }

    @PatchMapping("/{bookId}/publish")
    public ResponseEntity<Void> publishBook(@PathVariable Long bookId) {
        bookService.publishBook(bookId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{bookId}/unpublish")
    public ResponseEntity<Void> unpublishBook(@PathVariable Long bookId) {
        bookService.unpublishBook(bookId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long bookId) {
        bookService.deleteBook(bookId);
        return ResponseEntity.noContent().build();
    }
}
