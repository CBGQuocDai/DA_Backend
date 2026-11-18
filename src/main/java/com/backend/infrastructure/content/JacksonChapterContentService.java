package com.backend.infrastructure.content;

import com.backend.application.adapter.content.ChapterAttachment;
import com.backend.application.adapter.content.ChapterContentBuilder;
import com.backend.application.adapter.content.ChapterContentService;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Chapter;
import com.backend.domain.valueobject.BusinessError;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

@Component
public class JacksonChapterContentService implements ChapterContentService {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Override
    public ChapterContentBuilder newBuilder() {
        return new ChapterDocument();
    }

    @Override
    public String sanitize(String content) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        for (JsonNode block : readBlocks(content)) {
            if (ChapterDocument.lineOf(block) == null) {
                throw new BusinessException(BusinessError.CHAPTER_CONTENT_INVALID);
            }
            // Đường dẫn được ký URL khi đọc chương nên phải nằm trong thư mục ảnh
            if ("image".equals(typeOf(block)) && !isImagePath(block.path("attrs").path("path").asString(""))) {
                throw new BusinessException(BusinessError.CHAPTER_CONTENT_INVALID);
            }
        }
        return withImageUrls(content, path -> null);
    }

    @Override
    public String text(String content) {
        if (StringUtils.isBlank(content)) {
            return "";
        }
        List<String> lines = new ArrayList<>();
        for (JsonNode block : readBlocks(content)) {
            if ("pageBreak".equals(typeOf(block))) {
                continue;
            }
            String line = ChapterDocument.lineOf(block);
            if (line == null) {
                throw new BusinessException(BusinessError.CHAPTER_CONTENT_INVALID);
            }
            lines.add(line);
        }
        return String.join("\n", lines);
    }

    @Override
    public String withImageUrls(String content, UnaryOperator<String> urlOfPath) {
        if (StringUtils.isBlank(content)) {
            return content;
        }
        JsonNode doc = readDoc(content);
        boolean changed = false;
        for (JsonNode block : doc.path("content")) {
            if ("image".equals(typeOf(block)) && block.path("attrs") instanceof ObjectNode attrs) {
                String url = urlOfPath.apply(attrs.path("path").asString(""));
                if (url == null) {
                    changed |= attrs.remove("src") != null;
                } else {
                    attrs.put("src", url);
                    changed = true;
                }
            }
        }
        return changed ? JSON.writeValueAsString(doc) : content;
    }

    @Override
    public List<ChapterAttachment> attachments(String content) {
        if (StringUtils.isBlank(content)) {
            return List.of();
        }
        List<ChapterAttachment> attachments = new ArrayList<>();
        for (JsonNode block : readBlocks(content)) {
            JsonNode attrs = block.path("attrs");
            String caption = attrs.path("caption").asString("");
            switch (typeOf(block)) {
                case "image" -> attachments.add(
                        new ChapterAttachment.Image(attrs.path("path").asString(""), caption));
                case "table" -> attachments.add(new ChapterAttachment.Table(
                        rows(attrs.path("rows")), Math.max(0, attrs.path("headerRows").asInt(0)), caption));
                default -> {
                }
            }
        }
        return attachments;
    }

    @Override
    public String merge(List<Chapter> chapters) {
        ObjectNode doc = JSON.createObjectNode().put("type", "doc");
        ArrayNode blocks = doc.putArray("content");
        for (Chapter chapter : chapters) {
            if (chapter != chapters.getFirst()) {
                appendParagraph(blocks, chapter.getTitle());
            }
            if (!StringUtils.isBlank(chapter.getContent())) {
                readBlocks(chapter.getContent()).forEach(block -> blocks.add(block.deepCopy()));
            }
        }
        return JSON.writeValueAsString(doc);
    }

    private static List<List<String>> rows(JsonNode rowsNode) {
        List<List<String>> rows = new ArrayList<>();
        for (JsonNode row : rowsNode) {
            List<String> cells = new ArrayList<>();
            row.forEach(cell -> cells.add(cell.asString("")));
            rows.add(cells);
        }
        return rows;
    }

    private static void appendParagraph(ArrayNode blocks, String text) {
        ObjectNode paragraph = blocks.addObject().put("type", "paragraph");
        if (!text.isEmpty()) {
            paragraph.putArray("content").addObject().put("type", "text").put("text", text);
        }
    }

    private static String typeOf(JsonNode block) {
        return block.path("type").asString("");
    }

    private static boolean isImagePath(String path) {
        return path.startsWith(IMAGE_PATH_PREFIX) && !path.contains("..");
    }

    private static JsonNode readDoc(String content) {
        try {
            return JSON.readTree(content);
        } catch (JacksonException e) {
            throw new BusinessException(BusinessError.CHAPTER_CONTENT_INVALID);
        }
    }

    private static JsonNode readBlocks(String content) {
        JsonNode blocks = readDoc(content).path("content");
        if (!blocks.isArray()) {
            throw new BusinessException(BusinessError.CHAPTER_CONTENT_INVALID);
        }
        return blocks;
    }
}
