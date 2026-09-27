package com.backend.application.impl;


import com.backend.application.BookService;
import com.backend.domain.adapter.ocr.OcrService;
import com.backend.domain.adapter.repository.BookRepository;
import com.backend.domain.adapter.storage.StorageService;
import com.backend.domain.dto.request.book.BookCreateRequest;
import com.backend.domain.dto.response.PageResponse;
import com.backend.domain.mapper.BookMapper;
import com.backend.domain.model.Book;
import com.backend.domain.model.BookAuthor;
import com.backend.domain.model.BookCategory;
import com.backend.domain.model.Chapter;
import com.backend.domain.valueobject.BookStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final StorageService storageService;
    private final BookMapper bookMapper;
    private final OcrService ocrService;

    @Override
    public PageResponse<Book> getBooks(int page, int size) {
        PageResponse<Book> books = bookRepository.findAll(page, size);
        books.getContent().forEach(book -> {
            book.setCoverImage(storageService.createSignUrl(book.getCoverImage(),60L));
        });
        return books;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Book createBook(BookCreateRequest bookCreateRequest, MultipartFile pdfFile, MultipartFile bookCover) {
        // Validate PDF file (file content)
        if (pdfFile == null || !"application/pdf".equals(pdfFile.getContentType())) {
            throw new IllegalArgumentException("PDF file is required and must be of type application/pdf");
        }

        // Create Book with PROCESSING status
        Book book = bookMapper.toDomain(bookCreateRequest);
        book.setStatus(BookStatus.PROCESSING);

        // Set categories
        if (Objects.nonNull(bookCreateRequest.getCategories())) {
            book.setBookCategories(bookCreateRequest.getCategories().stream()
                    .map(cat -> BookCategory.builder().category(cat).build())
                    .toList());
        }

        // Set authors
        if (Objects.nonNull(bookCreateRequest.getAuthors())) {
            book.setBookAuthors(bookCreateRequest.getAuthors().stream()
                    .map(author -> BookAuthor.builder().author(author).build())
                    .toList());
        }

        // Insert Book to get ID
        book = bookRepository.insert(book);
        Long bookId = book.getId();

        // Save PDF to GCS: books/{bookId}/pdf/original.pdf
        String pdfPath = "books/" + bookId + "/pdf/original.pdf";
        storageService.store(pdfFile, pdfPath);
        book.setContentFile(pdfPath);

        // Save cover to GCS if provided: books/{bookId}/cover/cover.jpg
        if (bookCover != null && !bookCover.isEmpty()) {
            String coverPath = "books/" + bookId + "/cover/cover.jpg";
            storageService.store(bookCover, coverPath);
            book.setCoverImage(coverPath);
        }

        // Update book with coverImage and contentFile (single UPDATE query, no SELECT)
        bookRepository.update(book);

        // Trigger async OCR via OcrService (runs in separate thread)
        ocrService.processDocumentAsync(bookId, pdfPath);

        return book;
    }

    @Override
    public void updateBook(Book book) {
        bookRepository.update(book);
    }

    @Override
    public void deleteBook(Long bookId) {
        bookRepository.deleteById(bookId);
    }

    @Override
    public void publishBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + bookId));
        book.setStatus(BookStatus.PUBLISHED);
        bookRepository.update(book);
    }

    @Override
    public void unpublishBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + bookId));
        bookRepository.update(book);
    }
}
