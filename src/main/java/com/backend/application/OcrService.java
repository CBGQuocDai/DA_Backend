package com.backend.application;

import com.backend.domain.model.Chapter;
import java.util.List;

public interface OcrService {
    void processDocumentAsync(Long bookId, String pdfPath);
    List<Chapter> parseChaptersFromOcrResult(String ocrResult);
}
