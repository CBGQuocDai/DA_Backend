package com.backend.application.impl;


import com.backend.application.BookService;
import com.backend.application.OcrService;
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
    public Book createBook(BookCreateRequest bookCreateRequest, MultipartFile pdfFile, MultipartFile bookCover) {
        // Validate PDF file
        if (pdfFile == null || !"application/pdf".equals(pdfFile.getContentType())) {
            throw new IllegalArgumentException("PDF file is required and must be of type application/pdf");
        }

        // Create Book with PROCESSING status
        Book book = bookMapper.toDomain(bookCreateRequest);
        book.setStatus(BookStatus.PROCESSING);
        book.setIsPublish(false);

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

        // Save Book first to get ID
        book = bookRepository.save(book);
        Long bookId = book.getId();

        // Save PDF to GCS: books/{bookId}/pdf/original.pdf
        String pdfPath = "books/" + bookId + "/pdf/original.pdf";
        storageService.store(pdfFile, pdfPath);

        // Save cover to GCS if provided: books/{bookId}/cover/cover.jpg
        if (bookCover != null && !bookCover.isEmpty()) {
            String coverPath = "books/" + bookId + "/cover/cover.jpg";
            storageService.store(bookCover, coverPath);
            book.setCoverImage(coverPath);
        }

        // Trigger async OCR via OcrService
        ocrService.processDocumentAsync(bookId, pdfPath);

        return book;
    }

    @Override
    public Book updateBook(Book book) {
        return null;
    }

    @Override
    public void deleteBook(Book book) {

    }

    @Override
    public void publishBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + bookId));

        // Validate book has title
        if (book.getTitle() == null || book.getTitle().isBlank()) {
            throw new IllegalArgumentException("Book must have a title before publishing");
        }

        // Validate book has voice selected
        if (book.getVoice() == null) {
            throw new IllegalArgumentException("Book must have a voice selected before publishing");
        }

        // Validate book has at least one chapter
        if (book.getChapters() == null || book.getChapters().isEmpty()) {
            throw new IllegalArgumentException("Book must have at least one chapter before publishing");
        }

        // Validate all chapters have title and audioUrl
        for (int i = 0; i < book.getChapters().size(); i++) {
            Chapter chapter = book.getChapters().get(i);
            if (chapter.getTitle() == null || chapter.getTitle().isBlank()) {
                throw new IllegalArgumentException("Chapter at index " + i + " must have a title before publishing");
            }
            if (chapter.getAudioUrl() == null || chapter.getAudioUrl().isBlank()) {
                throw new IllegalArgumentException("Chapter '" + chapter.getTitle() + "' must have audio before publishing");
            }
        }

        // Update book status and publish flag
        book.setStatus(BookStatus.PUBLISHED);
        book.setIsPublish(true);

        bookRepository.save(book);
    }

    @Override
    public void unpublishBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + bookId));
        book.setIsPublish(false);
        bookRepository.save(book);
    }
}
