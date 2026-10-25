package com.backend.application.service.impl;


import com.backend.application.service.BookService;
import com.backend.application.validate.BookValidator;
import com.backend.domain.adapter.ocr.OcrService;
import com.backend.domain.adapter.repository.BookRepository;
import com.backend.domain.adapter.storage.StorageService;
import com.backend.domain.dto.request.book.BookCreateRequest;
import com.backend.domain.dto.response.PageResponse;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.mapper.BookMapper;
import com.backend.domain.model.Book;
import com.backend.domain.model.BookAuthor;
import com.backend.domain.model.BookCategory;
import com.backend.domain.model.BookStat;
import com.backend.domain.valueobject.BookStatus;
import com.backend.domain.valueobject.BusinessError;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final StorageService storageService;
    private final BookMapper bookMapper;
    private final OcrService ocrService;
    private final ObjectMapper objectMapper;
    private final BookValidator bookValidator;

    @Override
    public PageResponse<BookStat> getBooks(String bookName, Long categoryId, Long authorId, BookStatus status,
                                           int page, int size) {
        PageResponse<BookStat> bookStats = bookRepository.findAllWithStats(bookName, categoryId, authorId, status,
                page, size);

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
                .orElseThrow(() -> new BusinessException(BusinessError.BOOK_NOT_FOUND));
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
        BookCreateRequest req = parseBookInfo(bookCreateRequest);
        bookValidator.validateForCreate(req);
        bookValidator.validatePdfFile(pdfFile);
        bookValidator.validateCoverImage(bookCover);

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
        // PDF đã được upload lên GCS tại books/{bookId}/pdf/original.pdf
        ocrService.processDocumentAsync(book, pdfFile);
        return book;
    }

    @Override
    @Transactional
    public Book updateBook(Long bookId, String bookUpdateRequest, MultipartFile bookCover, MultipartFile contentFile) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException(BusinessError.BOOK_NOT_FOUND));

        BookCreateRequest req = parseBookInfo(bookUpdateRequest);
        bookValidator.validateForUpdate(req);
        if (bookCover != null && !bookCover.isEmpty()) {
            bookValidator.validateCoverImage(bookCover);
        }
        if (Objects.nonNull(contentFile) && !contentFile.isEmpty()) {
            bookValidator.validatePdfFile(contentFile);
        }

        if (req.getTitle() != null) {
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

        if (Objects.nonNull(contentFile) && !contentFile.isEmpty()) {
            String pdfPath = "books/" + bookId + "/pdf/original.pdf";
            storageService.store(contentFile, pdfPath);
            book.setContentFile(pdfPath);

            // TODO: chạy lại OCR cho contentFile mới (chưa chốt phương án xử lý chapter cũ):
            //  - xóa các chapter cũ của sách (chapterRepository.deleteByBookId) hoặc giữ lại
            //  - set book.setStatus(BookStatus.PROCESSING) rồi gọi ocrService.processDocumentAsync(book, contentFile)
        }

        bookRepository.update(book);
        return getBookById(bookId);
    }

    @Override
    @Transactional
    public void deleteBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException(BusinessError.BOOK_NOT_FOUND));

        // 1. Xóa file trên GCS trước (nếu lỗi GCS thì vẫn tiếp tục xóa DB để tránh orphan)
        if (book.getContentFile() != null) {
            try {
                storageService.delete(book.getContentFile());
            } catch (Exception ex) {
                log.warn("Không xóa được file PDF trên GCS cho book {}: {}",
                        bookId, ex.getMessage());
            }
        }
        if (book.getCoverImage() != null) {
            try {
                storageService.delete(book.getCoverImage());
            } catch (Exception ex) {
                log.warn("Không xóa được file cover trên GCS cho book {}: {}",
                        bookId, ex.getMessage());
            }
        }

        // 2. Xóa book + cascade các bảng phụ thuộc (chapter, favorite, author, ...)
        bookRepository.deleteById(bookId);
    }

    @Override
    public void publishBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException(BusinessError.BOOK_NOT_FOUND));
        book.setStatus(BookStatus.PUBLISHED);
        bookRepository.update(book);
    }

    @Override
    public void unpublishBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException(BusinessError.BOOK_NOT_FOUND));
        book.setStatus(BookStatus.UNPUBLISHED);
        bookRepository.update(book);
    }

    private BookCreateRequest parseBookInfo(String bookInfo) {
        if (bookInfo == null || bookInfo.isBlank()) {
            throw new BusinessException(BusinessError.MALFORMED_JSON);
        }
        try {
            return objectMapper.readValue(bookInfo, BookCreateRequest.class);
        } catch (Exception e) {
            log.warn("Failed to parse bookInfo: {}", e.getMessage());
            throw new BusinessException(BusinessError.MALFORMED_JSON);
        }
    }
}
