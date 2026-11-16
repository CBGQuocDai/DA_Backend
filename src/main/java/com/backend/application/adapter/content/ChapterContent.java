package com.backend.application.adapter.content;

/**
 * Nội dung của một chương: văn bản thuần TTS đọc và tài liệu biên tập (null khi chỉ có văn bản).
 * {@code heading} là dòng tiêu đề chèn trước nội dung khi gộp chương, null nếu không có.
 */
public record ChapterContent(String heading, String rawText, String contentJson) {
}
