package com.backend.infrastructure.ocr;

import com.backend.domain.model.Chapter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Tài liệu biên tập của một chương, dựng dần từng block. Ảnh và bảng là tệp đính kèm: tài liệu
 * giữ lại chúng, còn văn bản thuần (thứ TTS đọc) chỉ có một dòng trỏ tới chúng.
 */
public class ChapterDocument {

    public static final String IMAGE_TEXT = "Xem ảnh trong tệp đính kèm của chương.";
    public static final String TABLE_TEXT = "Xem bảng trong tệp đính kèm của chương.";
    /** Thư mục trên storage mà ảnh của tài liệu bắt buộc phải nằm trong. */
    public static final String IMAGE_PATH_PREFIX = "images/";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final List<ObjectNode> blocks = new ArrayList<>();

    public void paragraph(String text) {
        ObjectNode block = block("paragraph");
        if (!text.isEmpty()) {
            block.putArray("content").addObject().put("type", "text").put("text", text);
        }
    }

    public void pageBreak() {
        block("pageBreak");
    }

    public void image(String path, String description) {
        block("image").putObject("attrs").put("path", path).put("description", description);
    }

    public void table(List<List<String>> rows, int headerRows, String description) {
        ObjectNode attrs = block("table").putObject("attrs");
        ArrayNode rowsNode = attrs.putArray("rows");
        rows.forEach(row -> row.forEach(rowsNode.addArray()::add));
        attrs.put("headerRows", headerRows).put("description", description);
    }

    /** Có nội dung nào khác ngoài tệp đính kèm và dòng trống hay không. */
    public boolean hasText() {
        return blocks.stream().anyMatch(block -> !isAttachment(block) && !lineOf(block).isEmpty());
    }

    public boolean hasAttachment() {
        return blocks.stream().anyMatch(ChapterDocument::isAttachment);
    }

    public void clear() {
        blocks.clear();
    }

    /** Văn bản thuần, mỗi block một dòng, bỏ các dòng trống ở đầu và cuối tài liệu. */
    public String text() {
        return String.join("\n", trimmed().stream().map(ChapterDocument::lineOf).toList());
    }

    /** Tài liệu đúng như editor lưu, hoặc null khi văn bản thuần đã thể hiện đủ nội dung. */
    public String json() {
        if (!hasAttachment()) {
            return null;
        }
        ObjectNode doc = JSON.createObjectNode().put("type", "doc");
        doc.putArray("content").addAll(trimmed());
        return JSON.writeValueAsString(doc);
    }

    /** Dòng văn bản thuần ứng với một block, null nếu không nhận ra loại block. */
    public static String lineOf(JsonNode block) {
        return switch (block.path("type").asString("")) {
            case "pageBreak" -> Chapter.PAGE_BREAK;
            case "image" -> IMAGE_TEXT;
            case "table" -> TABLE_TEXT;
            case "paragraph" -> {
                StringBuilder line = new StringBuilder();
                block.path("content").forEach(child -> line.append(child.path("text").asString("")));
                yield line.toString();
            }
            default -> null;
        };
    }

    private ObjectNode block(String type) {
        ObjectNode block = JSON.createObjectNode().put("type", type);
        blocks.add(block);
        return block;
    }

    private List<ObjectNode> trimmed() {
        int start = 0;
        int end = blocks.size();
        while (start < end && lineOf(blocks.get(start)).isBlank()) {
            start++;
        }
        while (end > start && lineOf(blocks.get(end - 1)).isBlank()) {
            end--;
        }
        return blocks.subList(start, end);
    }

    private static boolean isAttachment(JsonNode block) {
        String type = block.path("type").asString("");
        return "image".equals(type) || "table".equals(type);
    }
}
