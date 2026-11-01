package com.backend.infrastructure.content;

import com.backend.application.adapter.content.ChapterContentParser;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Chapter;
import com.backend.domain.valueobject.BusinessError;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

@Component
public class JacksonChapterContentParser implements ChapterContentParser {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Override
    public String extractText(String contentJson) {
        JsonNode blocks;
        try {
            blocks = JSON.readTree(contentJson).path("content");
        } catch (JacksonException e) {
            throw new BusinessException(BusinessError.CHAPTER_CONTENT_INVALID);
        }
        if (!blocks.isArray()) {
            throw new BusinessException(BusinessError.CHAPTER_CONTENT_INVALID);
        }
        List<String> lines = new ArrayList<>();
        for (JsonNode block : blocks) {
            switch (block.path("type").asString("")) {
                case "pageBreak" -> lines.add(Chapter.PAGE_BREAK);
                case "paragraph" -> {
                    StringBuilder line = new StringBuilder();
                    block.path("content").forEach(child -> line.append(child.path("text").asString("")));
                    lines.add(line.toString());
                }
                default -> throw new BusinessException(BusinessError.CHAPTER_CONTENT_INVALID);
            }
        }
        return String.join("\n", lines);
    }
}
