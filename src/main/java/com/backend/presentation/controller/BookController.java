package com.backend.presentation.controller;


import com.backend.application.service.BookService;
import com.backend.application.service.CategoryService;
import com.backend.domain.dto.response.PageResponse;
import com.backend.domain.model.Book;
import com.backend.domain.model.BookStat;
import com.backend.domain.valueobject.BookStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.util.List;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;
    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<PageResponse<BookStat>> getListBooks(
            @RequestParam(required = false) String bookName,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) BookStatus status,
            @RequestParam Integer page,
            @RequestParam Integer size
    ) {
        return ResponseEntity.ok(bookService.getBooks(bookName, categoryId, authorId, status, page, size));
    }

    @GetMapping("/{bookId}")
    public ResponseEntity<com.backend.domain.dto.response.ApiResponse<Book>> getBookById(@PathVariable Long bookId) {
        return ResponseEntity.ok(com.backend.domain.dto.response.ApiResponse.<Book>builder()
                .data(bookService.getBookById(bookId))
                .build());
    }

    @GetMapping("/categories")
    public ResponseEntity<com.backend.domain.dto.response.ApiResponse<List<com.backend.domain.model.Category>>> getCategories() {
        return ResponseEntity.ok(com.backend.domain.dto.response.ApiResponse.<List<com.backend.domain.model.Category>>builder()
                .data(categoryService.getCategories())
                .build());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Book> createBook(
            @RequestPart("book") String request,
            @RequestPart("contentFile") MultipartFile contentFile,
            @RequestPart(value = "bookCover", required = false) MultipartFile bookCover
    ) {
        return ResponseEntity.ok(bookService.createBook(request, contentFile, bookCover));
    }

    @PutMapping(value = "/{bookId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<com.backend.domain.dto.response.ApiResponse<Book>> updateBook(
            @PathVariable Long bookId,
            @RequestPart(value = "book", required = false) String request,
            @RequestPart(value = "contentFile", required = false) MultipartFile contentFile,
            @RequestPart(value = "bookCover", required = false) MultipartFile bookCover
    ) {
        return ResponseEntity.ok(com.backend.domain.dto.response.ApiResponse.<Book>builder()
                .data(bookService.updateBook(bookId, request, bookCover, contentFile))
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
