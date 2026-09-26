package com.backend.domain.dto.request.book;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChapterSplitRequest {
    private int splitPosition;  // Character position to split at
}
