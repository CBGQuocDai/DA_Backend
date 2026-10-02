package com.backend.presentation.controller;


import com.backend.application.BookService;
import com.backend.domain.dto.request.book.BookCreateRequest;
import com.backend.domain.dto.response.PageResponse;
import com.backend.domain.model.Book;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;
    private final com.backend.application.CategoryService categoryService;

    @GetMapping
    public ResponseEntity<PageResponse<Book>> getListBooks(
            @RequestParam Integer page,
            @RequestParam Integer size
    ) {
        return ResponseEntity.ok(bookService.getBooks(page, size));
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
            @RequestPart("pdfFile") MultipartFile pdfFile,
            @RequestPart(value = "bookCover", required = false) MultipartFile bookCover
    ) {
        return ResponseEntity.ok(bookService.createBook(request, pdfFile, bookCover));
    }

    @PutMapping(value = "/{bookId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<com.backend.domain.dto.response.ApiResponse<Book>> updateBook(
            @PathVariable Long bookId,
            @RequestPart("book") String request,
            @RequestPart(value = "bookCover", required = false) MultipartFile bookCover
    ) {
        return ResponseEntity.ok(com.backend.domain.dto.response.ApiResponse.<Book>builder()
                .data(bookService.updateBook(bookId, request, bookCover))
                .build());
    }

    @PutMapping("/{bookId}/publish")
    public ResponseEntity<Void> publishBook(@PathVariable Long bookId) {
        bookService.publishBook(bookId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{bookId}/unpublish")
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
