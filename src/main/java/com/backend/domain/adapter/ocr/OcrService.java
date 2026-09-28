package com.backend.domain.adapter.ocr;

import com.backend.domain.model.Chapter;
import java.util.List;

/**
 * OCR Service adapter for external OCR service integration.
 * This is a port interface that defines the contract for OCR operations.
 */
public interface OcrService {
    void processDocumentAsync(Long bookId, String pdfPath);
}
