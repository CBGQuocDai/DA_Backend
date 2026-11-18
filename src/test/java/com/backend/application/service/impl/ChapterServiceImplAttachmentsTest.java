package com.backend.application.service.impl;

import com.backend.application.adapter.messaging.MessageQueue;
import com.backend.application.adapter.realtime.AdminEventPublisher;
import com.backend.application.adapter.repository.BookRepository;
import com.backend.application.adapter.repository.ChapterRepository;
import com.backend.application.adapter.repository.CustomerBookRepository;
import com.backend.application.adapter.storage.StorageService;
import com.backend.application.mapper.ResponseMapper;
import com.backend.application.service.FileManagementService;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.valueobject.PlaybackChapter;
import com.backend.infrastructure.content.JacksonChapterContentService;
import com.backend.infrastructure.pdf.PdfBoxService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChapterServiceImplAttachmentsTest {

    private static final long BOOK_ID = 1L;
    private static final long CHAPTER_ID = 10L;
    private static final String IMAGE = "{\"type\":\"image\",\"attrs\":{\"path\":\"images/books/1/a.png\","
            + "\"description\":\"Sơ đồ tổng quan\"}}";
    private static final String TABLE = "{\"type\":\"table\",\"attrs\":{\"rows\":[[\"Tên\",\"Giá trị\"],"
            + "[\"Độ dài\",\"Một ô rất dài cần xuống dòng nhiều lần để kiểm tra việc ngắt dòng trong bảng\"]],"
            + "\"headerRows\":1,\"description\":\"\"}}";

    private final ChapterRepository chapterRepository = mock(ChapterRepository.class);
    private final StorageService storage = mock(StorageService.class);
    private final ChapterServiceImpl service = new ChapterServiceImpl(
            chapterRepository, mock(BookRepository.class), mock(ResponseMapper.class), mock(MessageQueue.class),
            new JacksonChapterContentService(), mock(CustomerBookRepository.class),
            mock(FileManagementService.class), mock(AdminEventPublisher.class), new PdfBoxService(), storage);

    @Test
    void numbersAttachmentsAndPrefersEditorCaptions() throws IOException {
        when(storage.load(anyString())).thenReturn(png());
        String captioned = TABLE.replace("\"description\":\"\"", "\"caption\":\"Bảng 2.1: Thông số mô hình\",\"description\":\"\"");
        givenChapter("Chương 1: Mở đầu", doc(paragraph("bỏ qua"), IMAGE, captioned, IMAGE.replace("a.png", "b.png")));

        String text = pdfText(service.getAttachmentsPdf(null, true, BOOK_ID, CHAPTER_ID));

        assertThat(text).contains("Chương 1: Mở đầu", "Hình 1", "Bảng 2.1: Thông số mô hình", "Giá trị", "Hình 2")
                .doesNotContain("bỏ qua", "Bảng 1\n", "Sơ đồ tổng quan");
    }

    @Test
    void unreadableImageLeavesANotice() throws IOException {
        when(storage.load(anyString())).thenThrow(new IllegalStateException("not found"));
        givenChapter("C", doc(IMAGE, TABLE));

        String text = pdfText(service.getAttachmentsPdf(null, true, BOOK_ID, CHAPTER_ID));

        assertThat(text).contains("Hình 1", "Không hiển thị được ảnh.", "Bảng 1");
    }

    @Test
    void chapterWithoutAttachmentsIsRejected() {
        givenChapter("C", doc(paragraph("a")));

        assertThatThrownBy(() -> service.getAttachmentsPdf(null, true, BOOK_ID, CHAPTER_ID))
                .isInstanceOf(BusinessException.class);
    }

    private void givenChapter(String title, String content) {
        when(chapterRepository.findPlaybackChaptersByBookId(BOOK_ID))
                .thenReturn(List.of(new PlaybackChapter(CHAPTER_ID, title, 0, null, null, null)));
        when(chapterRepository.findContentById(CHAPTER_ID)).thenReturn(Optional.of(content));
    }

    private static String doc(String... blocks) {
        return "{\"type\":\"doc\",\"content\":[" + String.join(",", blocks) + "]}";
    }

    private static String paragraph(String text) {
        return "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"" + text + "\"}]}";
    }

    private static String pdfText(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private static byte[] png() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1200, 400, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }
}
