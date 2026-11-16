package com.backend.application.dto.response.book;

import com.backend.application.dto.response.chapter.CustomerChapterResponse;
import lombok.Builder;
import lombok.Getter;


// sửa extend BookResponse
@Getter
@Builder
public class CustomerBookOverviewResponse {
    private BookResponse book;
    private int chapterCount;
    /** Total audio duration in seconds, across all chapters. */
    private long totalDuration;
    private CustomerChapterResponse firstChapter;
}
