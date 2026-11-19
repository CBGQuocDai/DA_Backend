package com.backend.infrastructure.content;

import com.backend.application.adapter.content.ChapterAttachment;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Chapter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JacksonChapterContentServiceTest {

    private final JacksonChapterContentService service = new JacksonChapterContentService();

    private static String doc(String... blocks) {
        return "{\"type\":\"doc\",\"content\":[" + String.join(",", blocks) + "]}";
    }

    private static String paragraph(String text) {
        return "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"" + text + "\"}]}";
    }

    private static final String PAGE_BREAK = "{\"type\":\"pageBreak\"}";
    private static final String IMAGE = "{\"type\":\"image\",\"attrs\":{\"path\":\"images/books/1/a.png\","
            + "\"src\":\"https://signed\",\"description\":\"Sơ đồ tổng quan\"}}";
    private static final String TABLE = "{\"type\":\"table\",\"attrs\":{\"rows\":[[\"Tên\",\"Giá trị\"],"
            + "[\"Độ dài\",\"Một ô rất dài cần xuống dòng nhiều lần để kiểm tra việc ngắt dòng trong bảng\"]],"
            + "\"headerRows\":1,\"description\":\"\"}}";

    @Test
    void textSkipsPageBreaksAndReplacesAttachments() {
        String content = doc(paragraph("Dòng một"), PAGE_BREAK, IMAGE, paragraph("Dòng hai"), TABLE);

        assertThat(service.text(content)).isEqualTo(String.join("\n",
                "Dòng một", ChapterDocument.IMAGE_TEXT, "Dòng hai", ChapterDocument.TABLE_TEXT));
        assertThat(service.text(null)).isEmpty();
    }

    @Test
    void sanitizeStripsSignedUrlsAndRejectsUnknownBlocks() {
        assertThat(service.sanitize(doc(IMAGE))).doesNotContain("src").contains("images/books/1/a.png");
        assertThat(service.sanitize("  ")).isNull();
        assertThatThrownBy(() -> service.sanitize(doc("{\"type\":\"video\"}"))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.sanitize(
                doc("{\"type\":\"image\",\"attrs\":{\"path\":\"../secret\"}}"))).isInstanceOf(BusinessException.class);
    }

    @Test
    void attachmentsListsImagesAndTablesInOrderWithCaptions() {
        String captioned = TABLE.replace("\"description\":\"\"", "\"caption\":\"Bảng 2.1\",\"description\":\"\"");

        List<ChapterAttachment> attachments = service.attachments(doc(paragraph("bỏ qua"), IMAGE, captioned));

        assertThat(attachments).containsExactly(
                new ChapterAttachment.Image("images/books/1/a.png", ""),
                new ChapterAttachment.Table(List.of(List.of("Tên", "Giá trị"), List.of("Độ dài",
                        "Một ô rất dài cần xuống dòng nhiều lần để kiểm tra việc ngắt dòng trong bảng")),
                        1, "Bảng 2.1"));
        assertThat(service.attachments(null)).isEmpty();
    }

    @Test
    void mergePrefixesLaterChaptersWithTheirTitle() {
        List<Chapter> chapters = List.of(
                Chapter.builder().title("Chương 1").content(doc(paragraph("A"))).build(),
                Chapter.builder().title("Chương 2").content(null).build(),
                Chapter.builder().title("Chương 3").content(doc(IMAGE)).build());

        String merged = service.merge(chapters);

        assertThat(service.text(merged)).isEqualTo(String.join("\n",
                "A", "Chương 2", "Chương 3", ChapterDocument.IMAGE_TEXT));
    }

}
