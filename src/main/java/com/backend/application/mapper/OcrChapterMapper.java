package com.backend.application.mapper;

import com.backend.application.adapter.content.ChapterContentBuilder;
import com.backend.application.adapter.content.ChapterContentService;
import com.backend.application.adapter.ocr.OcrBlock;
import com.backend.domain.model.Chapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Chuyển các block OCR của một cuốn sách thành chương. Trang mở đầu bằng heading là trang tiêu đề của
 * chương mới; các heading đứng trước mọi đoạn văn trên trang đó ghép thành tiêu đề chương.
 */
@Component
@RequiredArgsConstructor
public class OcrChapterMapper {

    /** Tiêu đề của phần nội dung đứng trước heading đầu tiên. */
    public static final String DEFAULT_TITLE = "Mở đầu";
    private static final int MAX_TITLE_LENGTH = 255;

    private final ChapterContentService chapterContentService;

    /**
     * @param imagePaths đường dẫn trên storage của các ảnh đã lưu; ảnh không có trong map bị bỏ qua
     * @return các chương chưa gắn bookId, đánh thứ tự từ 0
     */
    public List<Chapter> toChapters(List<OcrBlock> blocks, Map<OcrBlock.Image, String> imagePaths) {
        Conversion conversion = new Conversion(chapterContentService.newBuilder(), imagePaths);
        blocks.forEach(conversion::accept);
        return conversion.finish();
    }

    /** Trạng thái của một lần chuyển: chương đang dựng và vị trí trong trang hiện tại. */
    private static final class Conversion {

        private final ChapterContentBuilder body;
        private final Map<OcrBlock.Image, String> imagePaths;
        private final List<Chapter> chapters = new ArrayList<>();
        private final List<String> titleParts = new ArrayList<>();
        private int currentPage;
        private boolean pageHasBody;
        private boolean pageHasHeading;
        private boolean pageBroken;

        private Conversion(ChapterContentBuilder body, Map<OcrBlock.Image, String> imagePaths) {
            this.body = body;
            this.imagePaths = imagePaths;
        }

        private void accept(OcrBlock block) {
            if (block.page() > currentPage) {
                closePage();
                currentPage = block.page();
                pageBroken = false;
                pageHasBody = false;
                pageHasHeading = false;
            }
            switch (block) {
                // Tệp đính kèm nằm giữa các heading của trang tiêu đề không kết thúc tiêu đề
                case OcrBlock.Image image -> {
                    String path = imagePaths.get(image);
                    if (path != null) {
                        body.image(path, image.description());
                    }
                }
                case OcrBlock.Table table -> body.table(table.rows(), table.headerRows(), table.description());
                case OcrBlock.Text text -> acceptText(text);
            }
        }

        private void acceptText(OcrBlock.Text text) {
            switch (text.kind()) {
                // Đầu trang lặp lại ở mọi trang, không phải nội dung
                case HEADER -> {
                }
                case FOOTER -> {
                    if (!pageBroken) {
                        breakPage();
                        pageBroken = true;
                    }
                }
                case HEADING, BODY -> {
                    if (text.text().isEmpty()) {
                        return;
                    }
                    if (text.kind() == OcrBlock.TextKind.HEADING && !pageHasBody) {
                        if (!pageHasHeading) {
                            addChapter(false);
                            pageHasHeading = true;
                        }
                        titleParts.add(text.text());
                    } else {
                        pageHasBody = true;
                        body.paragraph(text.text());
                    }
                }
            }
        }

        private List<Chapter> finish() {
            closePage();
            addChapter(true);
            return chapters;
        }

        /** Trang không có chân trang vẫn được ngắt khi sang trang mới. */
        private void closePage() {
            if (currentPage > 0 && !pageBroken && body.hasText()) {
                breakPage();
            }
        }

        private void breakPage() {
            body.paragraph("");
            body.pageBreak();
        }

        private void addChapter(boolean last) {
            if (!body.hasText() && !last) {
                // Tệp đính kèm không có văn bản đi cùng (hoa văn ở trang tiêu đề) được đưa sang đầu chương kế tiếp
                if (!body.hasAttachment()) {
                    body.clear();
                }
                return;
            }
            boolean isEmpty = !body.hasText() && !body.hasAttachment();
            if (isEmpty && titleParts.isEmpty()) {
                body.clear();
                return;
            }

            String title = titleParts.isEmpty() ? DEFAULT_TITLE : String.join(" - ", titleParts);
            if (title.length() > MAX_TITLE_LENGTH) {
                title = title.substring(0, MAX_TITLE_LENGTH);
            }
            if (isEmpty) {
                // Chương chỉ có tiêu đề vẫn cần nội dung để tạo audio
                body.paragraph(title);
            }
            chapters.add(Chapter.builder()
                    .title(title)
                    .readingSpeed(1D)
                    .chapterOrder(chapters.size())
                    .content(body.build())
                    .build());

            titleParts.clear();
            body.clear();
        }
    }
}
