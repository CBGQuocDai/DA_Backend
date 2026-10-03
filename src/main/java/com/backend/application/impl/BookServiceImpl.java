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
import com.backend.domain.model.BookStat;
import com.backend.domain.valueobject.BookStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final StorageService storageService;
    private final BookMapper bookMapper;
    private final OcrService ocrService;
    private final ObjectMapper objectMapper;

    @Override
    public PageResponse<BookStat> getBooks(int page, int size) {
        PageResponse<BookStat> bookStats = bookRepository.findAllWithStats(page, size);

        bookStats.getContent().forEach(bookStat -> {
            if (bookStat.getCoverImage() != null) {
                bookStat.setCoverImage(storageService.createSignUrl(bookStat.getCoverImage(), 60L));
            }
            if (bookStat.getContentFile() != null) {
                bookStat.setContentFile(storageService.createSignUrl(bookStat.getContentFile(), 60L));
            }
            if (bookStat.getVoice() != null && bookStat.getVoice().getExampleAudio() != null) {
                bookStat.getVoice().setExampleAudio(
                        storageService.createSignUrl(bookStat.getVoice().getExampleAudio(), 60L));
            }
        });
        return bookStats;
    }

    @Override
    public Book getBookById(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + bookId));
        if (book.getCoverImage() != null) {
            book.setCoverImage(storageService.createSignUrl(book.getCoverImage(), 60L));
        }
        if (book.getContentFile() != null) {
            book.setContentFile(storageService.createSignUrl(book.getContentFile(), 60L));
        }
        return book;
    }

    @Override
    @Transactional
    public Book createBook(String bookCreateRequest, MultipartFile pdfFile, MultipartFile bookCover) {
        BookCreateRequest req ;
        try {
            req = objectMapper.readValue(bookCreateRequest, BookCreateRequest.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse voiceInfo", e);
        }

        // Validate PDF file (file content)
        if (pdfFile == null || !"application/pdf".equals(pdfFile.getContentType())) {
            throw new IllegalArgumentException("PDF file is required and must be of type application/pdf");
        }

        // Create Book with PROCESSING status
        Book book = bookMapper.toDomain(req);
        book.setStatus(BookStatus.PROCESSING);

        // Set categories
        if (Objects.nonNull(req.getCategories())) {
            book.setCategories(req.getCategories().stream()
                    .map(cat -> BookCategory.builder().category(cat).build())
                    .toList());
        }

        // Set authors
        if (Objects.nonNull(req.getAuthors())) {
            book.setAuthors(req.getAuthors().stream()
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
        ocrService.processDocumentAsync(book, pdfFile);
        return book;
    }

    @Override
    @Transactional
    public Book updateBook(Long bookId, String bookUpdateRequest, MultipartFile bookCover) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + bookId));

        BookCreateRequest req;
        try {
            req = objectMapper.readValue(bookUpdateRequest, BookCreateRequest.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse book update request", e);
        }

        if (req.getTitle() != null && !req.getTitle().trim().isEmpty()) {
            book.setTitle(req.getTitle().trim());
        }
        if (req.getPrice() != null) {
            book.setPrice(req.getPrice());
        }
        if (req.getDescription() != null) {
            book.setDescription(req.getDescription());
        }
        if (req.getVoice() != null) {
            book.setVoice(req.getVoice());
        }
        if (req.getCategories() != null) {
            book.setCategories(req.getCategories().stream()
                    .map(cat -> BookCategory.builder().category(cat).build())
                    .toList());
        }
        if (req.getAuthors() != null) {
            book.setAuthors(req.getAuthors().stream()
                    .map(author -> BookAuthor.builder().author(author).build())
                    .toList());
        }

        if (bookCover != null && !bookCover.isEmpty()) {
            String coverPath = "books/" + bookId + "/cover/cover.jpg";
            storageService.store(bookCover, coverPath);
            book.setCoverImage(coverPath);
        }

        bookRepository.update(book);
        return getBookById(bookId);
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
        book.setStatus(BookStatus.UNPUBLISHED);
        bookRepository.update(book);
    }
}
