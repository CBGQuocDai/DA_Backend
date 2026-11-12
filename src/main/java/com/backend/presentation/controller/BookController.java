package com.backend.presentation.controller;

import com.backend.application.dto.request.book.BookRequest;
import com.backend.application.dto.response.PageResponse;
import com.backend.application.dto.response.book.BookResponse;
import com.backend.application.dto.response.book.CustomerBookOverviewResponse;
import com.backend.application.dto.response.chapter.CustomerChapterResponse;
import com.backend.application.service.BookService;
import com.backend.domain.valueobject.BestsellerPeriod;
import com.backend.domain.valueobject.BookStatus;
import com.backend.presentation.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;

    @PreAuthorize("hasRole('ADMIN')")
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

//    endpoint thông kê sách bán chạy
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/bestsellers")
    public ResponseEntity<ApiResponse<List<BookResponse>>> getBestsellers(
            @RequestParam(defaultValue = "MONTH") BestsellerPeriod period) {
        return ResponseEntity.ok(ApiResponse.<List<BookResponse>>builder()
                .data(bookService.getBestsellers(period))
                .build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{bookId}")
    public ResponseEntity<ApiResponse<BookResponse>> getBookById(@PathVariable Long bookId) {
        return ResponseEntity.ok(ApiResponse.<BookResponse>builder()
                .data(bookService.getBookById(bookId))
                .build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<BookResponse>> createBook(
            @Validated @RequestBody BookRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<BookResponse>builder()
                        .data(bookService.createBook(request))
                        .build()
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{bookId}")
    public ResponseEntity<ApiResponse<BookResponse>> updateBook(
            @PathVariable Long bookId,
            @Valid @RequestBody BookRequest request
    ) {
        return ResponseEntity.ok(com.backend.presentation.dto.ApiResponse.<BookResponse>builder()
                .data(bookService.updateBook(bookId, request))
                .build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{bookId}/publish")
    public ResponseEntity<Void> publishBook(@PathVariable Long bookId) {
        bookService.publishBook(bookId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{bookId}/unpublish")
    public ResponseEntity<Void> unpublishBook(@PathVariable Long bookId) {
        bookService.unpublishBook(bookId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long bookId) {
        bookService.deleteBook(bookId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/catalog")
    public ResponseEntity<ApiResponse<PageResponse<BookResponse>>> getCustomerBooks(
            @RequestParam(required = false) String bookName,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<BookResponse>>builder()
                .data(bookService.getCustomerBooks(bookName, categoryId, authorId, page, size)).build());
    }

    /** Default suggestions are the latest published books. */
    @GetMapping("/recommendations")
    public ResponseEntity<ApiResponse<List<BookResponse>>> getRecommendations() {
        return ResponseEntity.ok(ApiResponse.<List<BookResponse>>builder()
                .data(bookService.getCustomerBooks(null, null, null, 0, 10).getContent()).build());
    }

    @GetMapping("/{bookId}/overview")
    public ResponseEntity<ApiResponse<CustomerBookOverviewResponse>> getCustomerBookOverview(@PathVariable Long bookId) {
        return ResponseEntity.ok(ApiResponse.<CustomerBookOverviewResponse>builder()
                .data(bookService.getCustomerBookOverview(bookId)).build());
    }

    @GetMapping("/{bookId}/chapters/list")
    public ResponseEntity<ApiResponse<List<CustomerChapterResponse>>> getCustomerChapters(@PathVariable Long bookId) {
        return ResponseEntity.ok(ApiResponse.<List<CustomerChapterResponse>>builder()
                .data(bookService.getCustomerChapters(bookId)).build());
    }
}
