package com.backend.infrastructure.pdf;

import com.backend.application.adapter.pdf.PdfDocument;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Viết nội dung chảy từ trên xuống qua các trang A4: đoạn chữ tự xuống dòng, ảnh co cho vừa trang,
 * bảng chia đều cột và sang trang theo từng hàng. Dùng font Noto Sans nhúng sẵn để hiển thị tiếng Việt.
 */
final class PdfBoxDocument implements PdfDocument {

    private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
    private static final float MARGIN = 50;
    private static final float LINE_SPACING = 1.35f;
    private static final float CELL_PADDING = 4;
    private static final float HEADER_SHADE = 0.93f;
    private static final float CONTENT_WIDTH = PAGE_SIZE.getWidth() - 2 * MARGIN;
    private static final float CONTENT_HEIGHT = PAGE_SIZE.getHeight() - 2 * MARGIN;
    private static final float TITLE_SIZE = 16;
    private static final float HEADING_SIZE = 12;
    private static final float TEXT_SIZE = 10;
    private static final float HEADING_GAP = 16;
    private static final float BLOCK_GAP = 6;

    private final PDDocument document = new PDDocument();
    private final PDFont regular;
    private final PDFont bold;
    private PDPageContentStream stream;
    private float y;

    PdfBoxDocument() {
        try {
            regular = loadFont("fonts/NotoSans-Regular.ttf");
            bold = loadFont("fonts/NotoSans-Bold.ttf");
            newPage();
        } catch (IOException e) {
            close();
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void title(String text) {
        write(() -> text(text, bold, TITLE_SIZE));
    }

    @Override
    public void heading(String text) {
        y -= HEADING_GAP;
        write(() -> text(text, bold, HEADING_SIZE));
    }

    @Override
    public void paragraph(String text) {
        write(() -> text(text, regular, TEXT_SIZE));
    }

    @Override
    public void image(byte[] content, String name) {
        write(() -> {
            // Đọc ảnh trước khi chừa khoảng trống để ảnh lỗi không để lại khoảng trống thừa
            PDImageXObject image = PDImageXObject.createFromByteArray(document, content, name);
            y -= BLOCK_GAP;
            float scale = Math.min(1, Math.min(CONTENT_WIDTH / image.getWidth(), CONTENT_HEIGHT / image.getHeight()));
            float width = image.getWidth() * scale;
            float height = image.getHeight() * scale;
            ensureSpace(height);
            stream.drawImage(image, MARGIN + (CONTENT_WIDTH - width) / 2, y - height, width, height);
            y -= height;
        });
    }

    /** Hàng tiêu đề in đậm trên nền xám; hàng cao hơn một trang bị cắt bớt dòng. */
    @Override
    public void table(List<List<String>> rows, int headerRows) {
        y -= BLOCK_GAP;
        write(() -> drawTable(rows, headerRows, TEXT_SIZE));
    }

    @Override
    public byte[] toByteArray() {
        try {
            closeStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        try (document) {
            closeStream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private interface PdfAction {
        void run() throws IOException;
    }

    private static void write(PdfAction action) {
        try {
            action.run();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void text(String text, PDFont font, float size) throws IOException {
        for (String line : wrap(clean(text, font), font, size, CONTENT_WIDTH)) {
            ensureSpace(size * LINE_SPACING);
            drawText(line, font, size, MARGIN, y);
            y -= size * LINE_SPACING;
        }
    }

    private void drawTable(List<List<String>> rows, int headerRows, float size) throws IOException {
        int columns = rows.stream().mapToInt(List::size).max().orElse(0);
        if (columns == 0) {
            return;
        }
        float columnWidth = CONTENT_WIDTH / columns;
        float lineHeight = size * LINE_SPACING;
        int maxLines = (int) ((CONTENT_HEIGHT - 2 * CELL_PADDING) / lineHeight);

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            boolean header = rowIndex < headerRows;
            PDFont font = header ? bold : regular;
            List<List<String>> cells = new ArrayList<>();
            int lineCount = 1;
            for (int column = 0; column < columns; column++) {
                List<String> row = rows.get(rowIndex);
                String cell = column < row.size() ? row.get(column) : "";
                List<String> lines = wrap(clean(cell, font), font, size, columnWidth - 2 * CELL_PADDING);
                lines = lines.subList(0, Math.min(lines.size(), maxLines));
                cells.add(lines);
                lineCount = Math.max(lineCount, lines.size());
            }
            float rowHeight = lineCount * lineHeight + 2 * CELL_PADDING;
            ensureSpace(rowHeight);

            if (header) {
                stream.setNonStrokingColor(HEADER_SHADE);
                stream.addRect(MARGIN, y - rowHeight, CONTENT_WIDTH, rowHeight);
                stream.fill();
                stream.setNonStrokingColor(0f);
            }
            for (int column = 0; column < columns; column++) {
                float x = MARGIN + column * columnWidth;
                stream.addRect(x, y - rowHeight, columnWidth, rowHeight);
                float lineTop = y - CELL_PADDING;
                for (String line : cells.get(column)) {
                    drawText(line, font, size, x + CELL_PADDING, lineTop);
                    lineTop -= lineHeight;
                }
            }
            stream.setLineWidth(0.5f);
            stream.stroke();
            y -= rowHeight;
        }
    }

    private void drawText(String text, PDFont font, float size, float x, float top) throws IOException {
        if (text.isEmpty()) {
            return;
        }
        stream.beginText();
        stream.setFont(font, size);
        stream.newLineAtOffset(x, top - size);
        stream.showText(text);
        stream.endText();
    }

    private void ensureSpace(float height) throws IOException {
        if (y - height < MARGIN && y < PAGE_SIZE.getHeight() - MARGIN) {
            newPage();
        }
    }

    private void newPage() throws IOException {
        closeStream();
        PDPage page = new PDPage(PAGE_SIZE);
        document.addPage(page);
        stream = new PDPageContentStream(document, page);
        y = PAGE_SIZE.getHeight() - MARGIN;
    }

    private void closeStream() throws IOException {
        if (stream != null) {
            stream.close();
            stream = null;
        }
    }

    private PDFont loadFont(String resource) throws IOException {
        try (InputStream font = PdfBoxDocument.class.getClassLoader().getResourceAsStream(resource)) {
            if (font == null) {
                throw new IOException("Missing font resource " + resource);
            }
            return PDType0Font.load(document, font);
        }
    }

    /** Gom khoảng trắng về một dấu cách và thay ký tự font không có (emoji, ký tự điều khiển) bằng "?". */
    private static String clean(String text, PDFont font) {
        StringBuilder cleaned = new StringBuilder();
        text.strip().replaceAll("\\s+", " ").codePoints().forEach(codePoint -> {
            String character = Character.toString(codePoint);
            try {
                font.encode(character);
                cleaned.append(character);
            } catch (IOException | IllegalArgumentException e) {
                cleaned.append('?');
            }
        });
        return cleaned.toString();
    }

    /** Chia thành các dòng vừa {@code maxWidth}; từ dài hơn một dòng bị cắt theo ký tự. */
    private static List<String> wrap(String text, PDFont font, float size, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (width(candidate, font, size) <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
                continue;
            }
            if (!line.isEmpty()) {
                lines.add(line.toString());
                line.setLength(0);
            }
            for (int offset = 0; offset < word.length(); ) {
                int end = offset + Character.charCount(word.codePointAt(offset));
                while (end < word.length()
                        && width(word.substring(offset, end + Character.charCount(word.codePointAt(end))), font, size)
                        <= maxWidth) {
                    end += Character.charCount(word.codePointAt(end));
                }
                if (end < word.length()) {
                    lines.add(word.substring(offset, end));
                } else {
                    line.append(word, offset, end);
                }
                offset = end;
            }
        }
        if (!line.isEmpty() || lines.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private static float width(String text, PDFont font, float size) throws IOException {
        return font.getStringWidth(text) / 1000 * size;
    }
}
