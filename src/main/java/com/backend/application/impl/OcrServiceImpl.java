package com.backend.application.impl;

import com.backend.application.OcrService;
import com.backend.domain.adapter.repository.BookRepository;
import com.backend.domain.model.Book;
import com.backend.domain.model.Chapter;
import com.backend.domain.valueobject.BookStatus;
import com.backend.domain.valueobject.ChapterStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcrServiceImpl implements OcrService {

    private final BookRepository bookRepository;

    @Override
    @Async
    public void processDocumentAsync(Long bookId, String pdfPath) {
        log.info("Starting OCR for book: {}", bookId);
        try {
            // TODO: Call Google Document AI to extract text from PDF
            String ocrResult = extractTextFromPdf(pdfPath);

            // Parse OCR result into chapters
            List<Chapter> chapters = parseChaptersFromOcrResult(ocrResult);

            // Update book status to READY_FOR_EDIT
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + bookId));
            book.setStatus(BookStatus.READY_FOR_EDIT);
            bookRepository.save(book);

            log.info("OCR completed for book: {}, created {} chapters", bookId, chapters.size());
        } catch (Exception e) {
            log.error("OCR failed for book: {}", bookId, e);
            // Update book status to error state if needed
        }
    }

    @Override
    public List<Chapter> parseChaptersFromOcrResult(String ocrResult) {
        log.info("Parsing chapters from OCR result");
        // TODO: Implement actual chapter parsing logic using Document AI layout analysis
        // This should identify chapter headings and split content accordingly
        return List.of();
    }

    private String extractTextFromPdf(String pdfPath) {
        // TODO: Implement Google Document AI integration
        // - Upload PDF to Document AI
        // - Process with OCR processor
        // - Return extracted text
        log.info("Extracting text from PDF: {}", pdfPath);
        return "";
    }

    private Chapter createChapter(Book book, String title, int order, String content) {
        return Chapter.builder()
                .book(book)
                .title(title)
                .chapterOrder(order)
                .rawText(content)
                .status(ChapterStatus.PENDING_TEXT)
                .build();
    }
}
