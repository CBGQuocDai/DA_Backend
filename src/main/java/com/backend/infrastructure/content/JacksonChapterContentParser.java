package com.backend.infrastructure.content;

import com.backend.application.adapter.content.ChapterAttachment;
import com.backend.application.adapter.content.ChapterContent;
import com.backend.application.adapter.content.ChapterContentParser;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.valueobject.BusinessError;
import com.backend.infrastructure.ocr.ChapterDocument;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

@Component
public class JacksonChapterContentParser implements ChapterContentParser {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    // Khớp với cách trình soạn nhận ra dòng ngắt trang trong văn bản thuần (plainText.ts)
    private static final Pattern PAGE_BREAK_LINE =
            Pattern.compile("^\\s*-{3,}\\s*page_break\\s*-{3,}\\s*$", Pattern.CASE_INSENSITIVE);

    @Override
    public String extractText(String contentJson) {
        List<String> lines = new ArrayList<>();
        for (JsonNode block : readBlocks(contentJson)) {
            String line = ChapterDocument.lineOf(block);
            if (line == null) {
                throw new BusinessException(BusinessError.CHAPTER_CONTENT_INVALID);
            }
            if ("image".equals(block.path("type").asString(""))) {
                // Đường dẫn được ký URL khi đọc chương nên phải nằm trong thư mục ảnh.
                if (!isImagePath(block.path("attrs").path("path").asString(""))) {
                    throw new BusinessException(BusinessError.CHAPTER_CONTENT_INVALID);
                }
            }
            lines.add(line);
        }
        return String.join("\n", lines);
    }

    @Override
    public String withImageUrls(String contentJson, UnaryOperator<String> urlOfPath) {
        JsonNode doc;
        try {
            doc = JSON.readTree(contentJson);
        } catch (JacksonException e) {
            throw new BusinessException(BusinessError.CHAPTER_CONTENT_INVALID);
        }
        boolean changed = false;
        for (JsonNode block : doc.path("content")) {
            if ("image".equals(block.path("type").asString("")) && block.path("attrs") instanceof ObjectNode attrs) {
                String url = urlOfPath.apply(attrs.path("path").asString(""));
                if (url == null) {
                    changed |= attrs.remove("src") != null;
                } else {
                    attrs.put("src", url);
                    changed = true;
                }
            }
        }
        return changed ? JSON.writeValueAsString(doc) : contentJson;
    }

    @Override
    public List<ChapterAttachment> attachments(String contentJson) {
        if (contentJson == null || contentJson.isBlank()) {
            return List.of();
        }
        JsonNode blocks;
        try {
            blocks = JSON.readTree(contentJson).path("content");
        } catch (JacksonException e) {
            return List.of();
        }
        List<ChapterAttachment> attachments = new ArrayList<>();
        for (JsonNode block : blocks) {
            JsonNode attrs = block.path("attrs");
            switch (block.path("type").asString("")) {
                case "image" -> {
                    String path = attrs.path("path").asString("");
                    if (isImagePath(path)) {
                        attachments.add(new ChapterAttachment(ChapterAttachment.Type.IMAGE, path, null, 0));
                    }
                }
                case "table" -> {
                    List<List<String>> rows = new ArrayList<>();
                    for (JsonNode row : attrs.path("rows")) {
                        List<String> cells = new ArrayList<>();
                        row.forEach(cell -> cells.add(cell.asString("")));
                        rows.add(cells);
                    }
                    attachments.add(new ChapterAttachment(ChapterAttachment.Type.TABLE, null, rows,
                            Math.max(0, attrs.path("headerRows").asInt(0))));
                }
                default -> {
                }
            }
        }
        return attachments;
    }

    @Override
    public ChapterContent merge(List<ChapterContent> parts) {
        boolean hasDocument = parts.stream().anyMatch(part -> !isBlank(part.contentJson()));
        if (!hasDocument) {
            List<String> lines = new ArrayList<>();
            for (ChapterContent part : parts) {
                if (part.heading() != null) {
                    lines.add(part.heading());
                }
                if (!isBlank(part.rawText())) {
                    lines.add(part.rawText());
                }
            }
            return new ChapterContent(null, String.join("\n", lines), null);
        }

        ObjectNode doc = JSON.createObjectNode().put("type", "doc");
        ArrayNode blocks = doc.putArray("content");
        for (ChapterContent part : parts) {
            if (part.heading() != null) {
                appendParagraph(blocks, part.heading());
            }
            if (!isBlank(part.contentJson())) {
                readBlocks(part.contentJson()).forEach(block -> blocks.add(block.deepCopy()));
            } else if (!isBlank(part.rawText())) {
                // Mỗi dòng một block, giống cách trình soạn mở một chương chỉ có văn bản
                for (String line : part.rawText().split("\n", -1)) {
                    if (PAGE_BREAK_LINE.matcher(line).matches()) {
                        blocks.addObject().put("type", "pageBreak");
                    } else {
                        appendParagraph(blocks, line);
                    }
                }
            }
        }
        String contentJson = JSON.writeValueAsString(doc);
        return new ChapterContent(null, extractText(contentJson), contentJson);
    }

    private static void appendParagraph(ArrayNode blocks, String text) {
        ObjectNode paragraph = blocks.addObject().put("type", "paragraph");
        if (!text.isEmpty()) {
            paragraph.putArray("content").addObject().put("type", "text").put("text", text);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // Đường dẫn được ký URL khi đọc chương nên phải nằm trong thư mục ảnh
    private static boolean isImagePath(String path) {
        return path.startsWith(ChapterDocument.IMAGE_PATH_PREFIX) && !path.contains("..");
    }

    private static JsonNode readBlocks(String contentJson) {
        JsonNode blocks;
        try {
            blocks = JSON.readTree(contentJson).path("content");
        } catch (JacksonException e) {
            throw new BusinessException(BusinessError.CHAPTER_CONTENT_INVALID);
        }
        if (!blocks.isArray()) {
            throw new BusinessException(BusinessError.CHAPTER_CONTENT_INVALID);
        }
        return blocks;
    }
}
