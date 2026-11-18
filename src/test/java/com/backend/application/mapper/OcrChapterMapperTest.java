package com.backend.application.mapper;

import com.backend.application.adapter.content.ChapterAttachment;
import com.backend.application.adapter.ocr.OcrBlock;
import com.backend.application.adapter.ocr.OcrBlock.TextKind;
import com.backend.domain.model.Chapter;
import com.backend.infrastructure.content.ChapterDocument;
import com.backend.infrastructure.content.JacksonChapterContentService;
import org.junit.jupiter.api.Test;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OcrChapterMapperTest {

    private final JacksonChapterContentService content = new JacksonChapterContentService();
    private final OcrChapterMapper mapper = new OcrChapterMapper(content);

    @Test
    void headingsOnANewPageStartAChapterAndJoinIntoItsTitle() {
        List<Chapter> chapters = split(List.of(
                text(1, TextKind.BODY, "Lời nói đầu"),
                text(2, TextKind.HEADING, "Chương 1"),
                text(2, TextKind.HEADING, "Khởi đầu"),
                text(2, TextKind.BODY, "Nội dung"),
                // Heading sau đoạn văn trên cùng trang chỉ là một đoạn văn
                text(2, TextKind.HEADING, "Mục nhỏ")));

        assertThat(chapters).extracting(Chapter::getTitle)
                .containsExactly(OcrChapterMapper.DEFAULT_TITLE, "Chương 1 - Khởi đầu");
        assertThat(chapters).extracting(Chapter::getChapterOrder).containsExactly(0, 1);
        assertThat(content.text(chapters.get(0).getContent())).isEqualTo("Lời nói đầu\n");
        assertThat(content.text(chapters.get(1).getContent())).isEqualTo("Nội dung\nMục nhỏ\n");
    }

    @Test
    void headersAreDroppedAndFootersBreakThePage() {
        List<Chapter> chapters = split(List.of(
                text(1, TextKind.HEADER, "Tên sách"),
                text(1, TextKind.BODY, "Trang một"),
                text(1, TextKind.FOOTER, "1"),
                text(2, TextKind.HEADER, "Tên sách"),
                text(2, TextKind.BODY, "Trang hai")));

        assertThat(chapters).singleElement().satisfies(chapter -> {
            assertThat(chapter.getContent()).contains("pageBreak").doesNotContain("Tên sách");
            assertThat(content.text(chapter.getContent())).isEqualTo("Trang một\n\nTrang hai\n");
        });
    }

    @Test
    void imagesWithoutAStoredPathAreDropped() {
        OcrBlock.Image kept = new OcrBlock.Image(1, new byte[]{1}, "image/png", "Sơ đồ");
        OcrBlock.Image dropped = new OcrBlock.Image(1, new byte[]{2}, "image/bmp", "");
        Map<OcrBlock.Image, String> paths = new IdentityHashMap<>(Map.of(kept, "images/books/1/a.png"));

        List<Chapter> chapters = mapper.toChapters(List.of(text(1, TextKind.BODY, "Đoạn"), kept, dropped), paths);

        assertThat(content.attachments(chapters.getFirst().getContent()))
                .extracting(attachment -> ((ChapterAttachment.Image) attachment).path())
                .containsExactly("images/books/1/a.png");
    }

    @Test
    void headingOnlyPagesJoinTheNextChapterTitle() {
        List<Chapter> chapters = split(List.of(
                text(1, TextKind.HEADING, "Phần 1"),
                text(2, TextKind.HEADING, "Chương 1"),
                text(2, TextKind.BODY, "Nội dung")));

        assertThat(chapters).extracting(Chapter::getTitle).containsExactly("Phần 1 - Chương 1");
    }

    @Test
    void trailingTitleOnlyChapterReadsItsTitle() {
        List<Chapter> chapters = split(List.of(
                text(1, TextKind.BODY, "Nội dung"),
                text(2, TextKind.HEADING, "Phụ lục")));

        assertThat(chapters).extracting(Chapter::getTitle).containsExactly(OcrChapterMapper.DEFAULT_TITLE, "Phụ lục");
        assertThat(content.text(chapters.getLast().getContent())).isEqualTo("Phụ lục");
    }

    @Test
    void attachmentsOnATitlePageMoveToTheStartOfTheNextChapter() {
        OcrBlock.Image ornament = new OcrBlock.Image(2, new byte[]{1}, "image/png", "");
        List<Chapter> chapters = mapper.toChapters(List.of(
                text(1, TextKind.BODY, "Lời nói đầu"),
                text(2, TextKind.HEADING, "Chương 1"),
                ornament,
                text(2, TextKind.HEADING, "Khởi đầu"),
                text(3, TextKind.BODY, "Nội dung")),
                new IdentityHashMap<>(Map.of(ornament, "images/books/1/a.png")));

        assertThat(chapters).extracting(Chapter::getTitle)
                .containsExactly(OcrChapterMapper.DEFAULT_TITLE, "Chương 1 - Khởi đầu");
        assertThat(content.attachments(chapters.get(0).getContent())).isEmpty();
        assertThat(content.attachments(chapters.get(1).getContent())).hasSize(1);
        assertThat(content.text(chapters.get(1).getContent())).startsWith(ChapterDocument.IMAGE_TEXT + "\nNội dung");
    }

    @Test
    void trailingAttachmentsWithoutTextStayAChapter() {
        OcrBlock.Table table = new OcrBlock.Table(1, List.of(List.of("A", "B")), 0, "");

        List<Chapter> chapters = split(List.of(table));

        assertThat(chapters).singleElement().satisfies(chapter -> {
            assertThat(chapter.getTitle()).isEqualTo(OcrChapterMapper.DEFAULT_TITLE);
            assertThat(content.attachments(chapter.getContent())).hasSize(1);
            // Có tệp đính kèm thì không phải chương rỗng, không chèn tiêu đề vào nội dung
            assertThat(content.text(chapter.getContent())).isEqualTo(ChapterDocument.TABLE_TEXT);
        });
    }

    private List<Chapter> split(List<OcrBlock> blocks) {
        return mapper.toChapters(blocks, Map.of());
    }

    private static OcrBlock.Text text(int page, TextKind kind, String text) {
        return new OcrBlock.Text(page, kind, text);
    }
}
