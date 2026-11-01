package com.backend.application.adapter.content;

/**
 * Reads the editor document stored for a chapter.
 * This is a port interface; the document format stays out of the application layer.
 */
public interface ChapterContentParser {
    /**
     * Plain text of the document: one line per block, matching what the editor reports as its text.
     *
     * @throws com.backend.domain.exception.BusinessException if the document is not valid
     */
    String extractText(String contentJson);
}
