package com.backend.domain.dto.request.book;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class ChapterMergeRequest {
    private List<Long> chapterIds;
    private String newTitle;
}
