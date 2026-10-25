package com.backend.application.validate;

import com.backend.domain.adapter.util.FileTypeDetector;
import com.backend.domain.dto.request.book.BookCreateRequest;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Voice;
import com.backend.domain.valueobject.BusinessError;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class BookValidator {

    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;
    private static final BigDecimal MIN_PRICE = BigDecimal.ZERO;
    private static final long MAX_COVER_SIZE = 5L * 1024L * 1024L; // 5 MB
    private static final long MAX_PDF_SIZE = 100L * 1024L * 1024L; // 100 MB
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final Set<String> ALLOWED_COVER_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final FileTypeDetector fileTypeDetector;

    /** Full validation for the create flow: every field is required. */
    public void validateForCreate(BookCreateRequest req) {
        validateTitle(req.getTitle());
        validateDescription(req.getDescription());
        validatePrice(req.getPrice());
        validateVoice(req.getVoice());
        validateCategories(req.getCategories());
        validateAuthors(req.getAuthors());
    }

    /** Partial validation for the update flow — only fields actually supplied are checked. */
    public void validateForUpdate(BookCreateRequest req) {
        if (req.getTitle() != null) {
            validateTitle(req.getTitle());
        }
        if (req.getDescription() != null) {
            validateDescription(req.getDescription());
        }
        if (req.getPrice() != null) {
            validatePrice(req.getPrice());
        }
        if (req.getVoice() != null) {
            validateVoice(req.getVoice());
        }
        if (req.getCategories() != null) {
            validateCategories(req.getCategories());
        }
        if (req.getAuthors() != null) {
            validateAuthors(req.getAuthors());
        }
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new BusinessException(BusinessError.BOOK_TITLE_REQUIRED);
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new BusinessException(BusinessError.BOOK_TITLE_TOO_LONG);
        }
    }

    private void validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new BusinessException(BusinessError.BOOK_DESCRIPTION_REQUIRED);
        }
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BusinessException(BusinessError.BOOK_DESCRIPTION_TOO_LONG);
        }
    }

    private void validatePrice(BigDecimal price) {
        if (price == null) {
            throw new BusinessException(BusinessError.BOOK_PRICE_REQUIRED);
        }
        if (price.compareTo(MIN_PRICE) < 0) {
            throw new BusinessException(BusinessError.BOOK_PRICE_INVALID);
        }
    }

    private void validateVoice(Voice voice) {
        if (voice == null || voice.getId() == null) {
            throw new BusinessException(BusinessError.BOOK_VOICE_REQUIRED);
        }
    }

    private void validateCategories(Collection<?> categories) {
        if (categories == null || categories.isEmpty() || categories.stream().anyMatch(c -> c == null)) {
            throw new BusinessException(BusinessError.BOOK_CATEGORY_REQUIRED);
        }
    }

    private void validateAuthors(Collection<?> authors) {
        if (authors == null || authors.isEmpty() || authors.stream().anyMatch(a -> a == null)) {
            throw new BusinessException(BusinessError.BOOK_AUTHOR_REQUIRED);
        }
    }

    /** Validate the book PDF: must exist, be a real PDF by byte sniffing, and be at most 100 MB. */
    public void validatePdfFile(MultipartFile pdfFile) {
        if (pdfFile == null || pdfFile.isEmpty()) {
            throw new BusinessException(BusinessError.BOOK_PDF_REQUIRED);
        }
        if (!PDF_CONTENT_TYPE.equals(fileTypeDetector.detectContentType(pdfFile))) {
            throw new BusinessException(BusinessError.BOOK_PDF_FORMAT_INCORRECT);
        }
        if (pdfFile.getSize() > MAX_PDF_SIZE) {
            throw new BusinessException(BusinessError.BOOK_PDF_TOO_LARGE);
        }
    }

    /** Validate the cover image: must exist, be a real JPG/PNG/WebP by byte sniffing, and be at most 5 MB. */
    public void validateCoverImage(MultipartFile cover) {
        if (cover == null || cover.isEmpty()) {
            throw new BusinessException(BusinessError.BOOK_COVER_REQUIRED);
        }
        if (!ALLOWED_COVER_TYPES.contains(fileTypeDetector.detectContentType(cover))) {
            throw new BusinessException(BusinessError.BOOK_COVER_FORMAT_INCORRECT);
        }
        if (cover.getSize() > MAX_COVER_SIZE) {
            throw new BusinessException(BusinessError.BOOK_COVER_TOO_LARGE);
        }
    }
}
