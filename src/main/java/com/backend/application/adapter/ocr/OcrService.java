package com.backend.application.adapter.ocr;

import com.backend.domain.model.Book;

/**
 * OCR Service adapter for external OCR service integration.
 * This is a port interface that defines the contract for OCR operations.
 */
public interface OcrService {
    /** Reads the book PDF from storage ({@link Book#getContentFile()}) and extracts its chapters. */
    void processDocumentAsync(Book book);
}
