package com.backend.application.service.impl;


import com.backend.application.service.BookService;
import com.backend.application.service.FileManagementService;
import com.backend.application.adapter.ocr.OcrService;
import com.backend.application.adapter.repository.BookRepository;
import com.backend.application.adapter.storage.StorageService;
import com.backend.application.dto.request.book.BookRequest;
import com.backend.application.dto.response.PageResponse;
import com.backend.application.dto.response.book.BookResponse;
import com.backend.application.mapper.ResponseMapper;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Author;
import com.backend.domain.model.Book;
import com.backend.domain.model.BookAuthor;
import com.backend.domain.model.BookCategory;
import com.backend.domain.model.Category;
import com.backend.domain.model.Voice;
import com.backend.domain.valueobject.BookStatus;
import com.backend.domain.valueobject.BusinessError;
import com.backend.domain.valueobject.UploadType;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final StorageService storageService;
    private final OcrService ocrService;
    private final FileManagementService fileManagementService;
    private final ResponseMapper responseMapper;

    @Override
    public PageResponse<BookResponse> getBooks(String bookName, Long categoryId, Long authorId, BookStatus status,
                                               int page, int size) {
        return responseMapper.toPageResponse(
                bookRepository.findAllWithStats(bookName, categoryId, authorId, status, page, size),
                responseMapper::toBookResponse);
    }

    @Override
    public PageResponse<BookResponse> getBooksByCategory(Long categoryId, int page, int size) {
        return responseMapper.toPageResponse(
                bookRepository.findByCategoryId(categoryId, page, size), responseMapper::toBookResponse);
    }

    @Override
    public BookResponse getBookById(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException(BusinessError.BOOK_NOT_FOUND));
        return responseMapper.toBookResponse(book);
    }

    /** Verifies before saving and moves after: a failed save keeps the files in tmp/, a failed move rolls it back. */
    @Override
    @Transactional
    public BookResponse createBook(BookRequest req) {
        fileManagementService.verifyUpload(req.getContentFile(), UploadType.PDF);
        fileManagementService.verifyUpload(req.getCoverImage(), UploadType.IMAGE);

        Book book = new Book();
        applyRequest(book, req);
        book.setStatus(BookStatus.PROCESSING);
        book = bookRepository.insert(book);
        fileManagementService.moveToPermanent(req.getContentFile(), UploadType.PDF);
        fileManagementService.moveToPermanent(req.getCoverImage(), UploadType.IMAGE);

        // Trigger async OCR via OcrService (runs in separate thread, reads the PDF from GCS)
        ocrService.processDocumentAsync(book);
        return responseMapper.toBookResponse(book);
    }

    @Override
    @Transactional
    public BookResponse updateBook(Long bookId, BookRequest req) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException(BusinessError.BOOK_NOT_FOUND));
        String oldCover = book.getCoverImage();
        String oldContent = book.getContentFile();

        verifyIfNew(req.getContentFile(), oldContent, UploadType.PDF);
        verifyIfNew(req.getCoverImage(), oldCover, UploadType.IMAGE);
        applyRequest(book, req);
        // TODO: khi contentFile đổi, chạy lại OCR (chưa chốt phương án xử lý chapter cũ):
        //  - xóa các chapter cũ của sách (chapterRepository.deleteByBookId) hoặc giữ lại
        //  - set book.setStatus(BookStatus.PROCESSING) rồi gọi ocrService.processDocumentAsync(book)

        bookRepository.update(book);
        moveIfNew(req.getContentFile(), oldContent, UploadType.PDF);
        moveIfNew(req.getCoverImage(), oldCover, UploadType.IMAGE);
        deleteReplacedFile(oldCover, book.getCoverImage(), bookId);
        deleteReplacedFile(oldContent, book.getContentFile(), bookId);
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

    /** Shared by create and update; the DTO is already validated and new files were verified. */
    private void applyRequest(Book book, BookRequest req) {
        book.setTitle(req.getTitle().trim());
        book.setPrice(req.getPrice());
        book.setDescription(req.getDescription());
        book.setVoice(Voice.builder().id(req.getVoiceId()).build());
        book.setCategories(toBookCategories(req.getCategoryIds()));
        book.setAuthors(toBookAuthors(req.getAuthorIds()));
        book.setContentFile(resolveFile(req.getContentFile(), book.getContentFile(), UploadType.PDF));
        book.setCoverImage(resolveFile(req.getCoverImage(), book.getCoverImage(), UploadType.IMAGE));
    }

    private String resolveFile(String requested, String current, UploadType type) {
        return isNewFile(requested, current) ? type.permanentPath(requested) : current;
    }

    private boolean isNewFile(String requested, String current) {
        return !requested.equals(current);
    }

    private void verifyIfNew(String requested, String current, UploadType type) {
        if (isNewFile(requested, current)) {
            fileManagementService.verifyUpload(requested, type);
        }
    }

    private void moveIfNew(String requested, String current, UploadType type) {
        if (isNewFile(requested, current)) {
            fileManagementService.moveToPermanent(requested, type);
        }
    }

    private List<BookCategory> toBookCategories(List<Long> categoryIds) {
        return categoryIds.stream()
                .map(id -> BookCategory.builder().category(Category.builder().id(id).build()).build())
                .toList();
    }

    private List<BookAuthor> toBookAuthors(List<Long> authorIds) {
        return authorIds.stream()
                .map(id -> BookAuthor.builder().author(Author.builder().id(id).build()).build())
                .toList();
    }

    /** Every file has its own path, so a replaced file must be removed explicitly. */
    private void deleteReplacedFile(String oldPath, String newPath, Long bookId) {
        if (oldPath == null || oldPath.equals(newPath)) {
            return;
        }
        try {
            storageService.delete(oldPath);
        } catch (Exception ex) {
            log.warn("Không xóa được file cũ {} trên GCS cho book {}: {}", oldPath, bookId, ex.getMessage());
        }
    }
}
