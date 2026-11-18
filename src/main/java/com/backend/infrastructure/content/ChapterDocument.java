package com.backend.infrastructure.content;

import com.backend.application.adapter.content.ChapterContentBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Tài liệu biên tập của một chương, dựng dần từng block. Ảnh và bảng là tệp đính kèm: tài liệu
 * giữ lại chúng, còn văn bản TTS đọc chỉ có một dòng trỏ tới chúng.
 */
public class ChapterDocument implements ChapterContentBuilder {

    public static final String IMAGE_TEXT = "Xem ảnh trong tệp đính kèm của chương.";
    public static final String TABLE_TEXT = "Xem bảng trong tệp đính kèm của chương.";
    /** Dòng đại diện cho dấu ngắt trang; không rỗng để trang chỉ có dấu ngắt vẫn được tính là có nội dung. */
    static final String PAGE_BREAK_LINE = "-------------page_break------------------";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final List<ObjectNode> blocks = new ArrayList<>();

    @Override
    public void paragraph(String text) {
        ObjectNode block = block("paragraph");
        if (!text.isEmpty()) {
            block.putArray("content").addObject().put("type", "text").put("text", text);
        }
    }

    @Override
    public void pageBreak() {
        block("pageBreak");
    }

    @Override
    public void image(String path, String description) {
        block("image").putObject("attrs").put("path", path).put("description", description);
    }

    @Override
    public void table(List<List<String>> rows, int headerRows, String description) {
        ObjectNode attrs = block("table").putObject("attrs");
        ArrayNode rowsNode = attrs.putArray("rows");
        rows.forEach(row -> row.forEach(rowsNode.addArray()::add));
        attrs.put("headerRows", headerRows).put("description", description);
    }

    @Override
    public boolean hasText() {
        return blocks.stream().anyMatch(block -> !isAttachment(block) && !lineOf(block).isEmpty());
    }

    @Override
    public boolean hasAttachment() {
        return blocks.stream().anyMatch(ChapterDocument::isAttachment);
    }

    @Override
    public void clear() {
        blocks.clear();
    }

    /** Tài liệu đúng như editor lưu, bỏ các dòng trống ở đầu và cuối. */
    @Override
    public String build() {
        ObjectNode doc = JSON.createObjectNode().put("type", "doc");
        doc.putArray("content").addAll(trimmed());
        return JSON.writeValueAsString(doc);
    }

    /** Dòng văn bản ứng với một block, null nếu không nhận ra loại block. */
    public static String lineOf(JsonNode block) {
        return switch (block.path("type").asString("")) {
            case "pageBreak" -> PAGE_BREAK_LINE;
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

    private static boolean isAttachment(JsonNode block) {
        String type = block.path("type").asString("");
        return "image".equals(type) || "table".equals(type);
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
}
