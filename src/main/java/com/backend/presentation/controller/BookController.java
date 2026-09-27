package com.backend.presentation.controller;


import com.backend.application.BookService;
import com.backend.domain.dto.request.book.BookCreateRequest;
import com.backend.domain.dto.response.PageResponse;
import com.backend.domain.model.Book;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @GetMapping
    public ResponseEntity<PageResponse<Book>> getListBooks(
            @RequestParam Integer page,
            @RequestParam Integer size
    ) {

        return ResponseEntity.ok(bookService.getBooks(page, size));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Book> createBook(
            @RequestPart("book") BookCreateRequest request,
            @RequestPart("pdfFile") MultipartFile pdfFile,
            @RequestPart(value = "bookCover", required = false) MultipartFile bookCover
    ) throws java.io.IOException {
        return ResponseEntity.ok(bookService.createBook(request, pdfFile, bookCover));
    }

    @PutMapping("/{bookId}/publish")
    public ResponseEntity<Void> publishBook(@PathVariable Long bookId) {
        bookService.publishBook(bookId);
        return ResponseEntity.ok().build();
    }
}
