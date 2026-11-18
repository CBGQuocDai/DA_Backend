package com.backend.application.service.impl;

import com.backend.application.adapter.content.ChapterContentService;
import com.backend.application.adapter.ocr.OcrBlock;
import com.backend.application.adapter.ocr.OcrService;
import com.backend.application.adapter.realtime.AdminEventPublisher;
import com.backend.application.adapter.repository.BookRepository;
import com.backend.application.adapter.repository.ChapterRepository;
import com.backend.application.adapter.storage.StorageService;
import com.backend.application.dto.request.book.BookRequest;
import com.backend.application.dto.response.PageResponse;
import com.backend.application.dto.response.book.BookResponse;
import com.backend.application.dto.response.book.CustomerBookOverviewResponse;
import com.backend.application.dto.response.chapter.CustomerChapterResponse;
import com.backend.application.mapper.OcrChapterMapper;
import com.backend.application.mapper.ResponseMapper;
import com.backend.application.service.BookService;
import com.backend.application.service.FileManagementService;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Author;
import com.backend.domain.model.Book;
import com.backend.domain.model.BookAuthor;
import com.backend.domain.model.BookCategory;
import com.backend.domain.model.Category;
import com.backend.domain.model.Chapter;
import com.backend.domain.model.Voice;
import com.backend.domain.valueobject.BestsellerPeriod;
import com.backend.domain.valueobject.BookStatus;
import com.backend.domain.valueobject.BusinessError;
import com.backend.domain.valueobject.UploadType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.backend.application.service.impl.TransactionHooks.afterCommit;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookServiceImpl implements BookService {

    /** Books listed in the dashboard's bestseller section. */
    private static final int BESTSELLER_LIMIT = 10;
    private static final Map<String, String> OCR_IMAGE_EXTENSIONS = Map.of(
            "image/png", "png", "image/jpeg", "jpg", "image/webp", "webp", "image/gif", "gif");

    private final BookRepository bookRepository;
    private final ChapterRepository chapterRepository;
    private final StorageService storageService;
    private final OcrService ocrService;
    private final OcrChapterMapper ocrChapterMapper;
    private final AdminEventPublisher adminEventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final TaskExecutor ocrTaskExecutor;
    private final FileManagementService fileManagementService;
    private final ResponseMapper responseMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookResponse> getCustomerBooks(String bookName, Long categoryId, Long authorId, int page, int size) {
        return responseMapper.toPageResponse(
                bookRepository.findAllWithStats(bookName, categoryId, authorId, BookStatus.PUBLISHED, page, size),
                responseMapper::toBookResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerBookOverviewResponse getCustomerBookOverview(Long bookId) {
        Book book = bookRepository.findBookWithMetadataById(bookId)
                .filter(candidate -> candidate.getStatus() == BookStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(BusinessError.BOOK_NOT_FOUND));
        var chapters = chapterRepository.findPlaybackChaptersByBookId(bookId);
        return CustomerBookOverviewResponse.builder()
                .book(responseMapper.toBookResponse(book))
                .chapterCount(chapters.size())
                .totalDuration(chapters.stream().mapToLong(chapter -> chapter.duration() == null ? 0 : chapter.duration()).sum())
                .firstChapter(chapters.isEmpty() ? null : responseMapper.toCustomerChapter(chapters.getFirst()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerChapterResponse> getCustomerChapters(Long bookId) {
        bookRepository.findStatusById(bookId)
                .filter(status -> status == BookStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(BusinessError.BOOK_NOT_FOUND));
        // Trình nghe chỉ cần metadata, không tải văn bản và tài liệu biên tập của cả sách.
        return chapterRepository.findPlaybackChaptersByBookId(bookId).stream().map(responseMapper::toCustomerChapter).toList();
    }

    @Override
    public PageResponse<BookResponse> getBooks(String bookName, Long categoryId, Long authorId, BookStatus status,
                                               int page, int size) {
        return responseMapper.toPageResponse(
                bookRepository.findAllWithStats(bookName, categoryId, authorId, status, page, size),
                responseMapper::toBookResponse);
    }

    @Override
    public BookResponse getBookById(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException(BusinessError.BOOK_NOT_FOUND));

        book.getChapters().forEach(c -> c.setAudioUrl(
                storageService.createSignUrl(c.getAudioUrl(), 60L)
        ));
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
        Book saved = bookRepository.insert(book);
        fileManagementService.moveToPermanent(req.getContentFile(), UploadType.PDF);
        fileManagementService.moveToPermanent(req.getCoverImage(), UploadType.IMAGE);
        // OCR mất nhiều thời gian: chạy ở thread riêng, và chỉ khi sách đã commit để thread đó thấy được sách
        afterCommit(() -> ocrTaskExecutor.execute(() -> processContent(saved)));
        return responseMapper.toBookResponse(saved);
    }

    /** OCR -> lưu ảnh -> chia chương -> lưu chương và chuyển sách sang UNPUBLISHED -> báo cho trang admin. */
    private void processContent(Book book) {
        // Ảnh đã upload, để xoá đi nếu không lưu được chương; so khớp theo đúng block chứ không theo nội dung
        Map<OcrBlock.Image, String> imagePaths = new IdentityHashMap<>();
        try {
            List<OcrBlock> blocks = ocrService.recognize(storageService.load(book.getContentFile()));
            storeImages(book.getId(), blocks, imagePaths);
            List<Chapter> chapters = ocrChapterMapper.toChapters(blocks, imagePaths);
            if (chapters.isEmpty()) {
                throw new IllegalStateException("No chapters extracted from document");
            }
            chapters.forEach(chapter -> chapter.setBookId(book.getId()));
            transactionTemplate.executeWithoutResult(status -> {
                chapterRepository.saveAll(chapters);
                book.setStatus(BookStatus.UNPUBLISHED);
                bookRepository.update(book);
            });
            log.info("Saved {} chapters for book {}", chapters.size(), book.getId());
        } catch (Exception e) {
            log.error("Error processing document for book: {}", book.getId(), e);
            imagePaths.values().forEach(this::deleteOcrImage);
            book.setStatus(BookStatus.FAILED);
            bookRepository.update(book);
        }
        adminEventPublisher.bookStatusChanged(book.getId(), book.getStatus());
    }

    /** Lưu các ảnh OCR lên storage, ghi đường dẫn vào {@code paths}; ảnh có định dạng không hỗ trợ bị bỏ qua. */
    private void storeImages(Long bookId, List<OcrBlock> blocks, Map<OcrBlock.Image, String> paths) {
        String folder = ChapterContentService.IMAGE_PATH_PREFIX + "books/" + bookId + "/";
        for (OcrBlock block : blocks) {
            if (!(block instanceof OcrBlock.Image image)) {
                continue;
            }
            String extension = OCR_IMAGE_EXTENSIONS.get(image.mimeType());
            if (extension == null) {
                log.warn("Skipping OCR image with unsupported mime type '{}'", image.mimeType());
                continue;
            }
            String path = folder + UUID.randomUUID() + "." + extension;
            storageService.store(image.content(), path, image.mimeType());
            paths.put(image, path);
        }
    }

    private void deleteOcrImage(String path) {
        try {
            storageService.delete(path);
        } catch (Exception e) {
            log.warn("Could not remove OCR image {}", path, e);
        }
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
        //  - set book.setStatus(BookStatus.PROCESSING) rồi chạy processContent(book) trên ocrTaskExecutor sau khi commit

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
        book.getChapters().forEach(c -> {
            if (Objects.isNull(c.getAudioUrl()) || StringUtils.isBlank(c.getContent())) {
                throw new BusinessException(BusinessError.BOOK_ERROR_PUBLISH);
            }
        });
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
        book.setCategories(req.getCategoryIds().stream()
                .map(id -> BookCategory.builder().category(Category.builder().id(id).build()).build())
                .toList());
        book.setAuthors(req.getAuthorIds().stream()
                .map(id -> BookAuthor.builder().author(Author.builder().id(id).build()).build())
                .toList());
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

    @Override
    @Transactional(readOnly = true)
    public List<BookResponse> getBestsellers(BestsellerPeriod period) {
        return bookRepository.findBestsellers(period.endingAt(Instant.now()), BESTSELLER_LIMIT)
                .stream().map(responseMapper::toBookResponse).toList();
    }
}
