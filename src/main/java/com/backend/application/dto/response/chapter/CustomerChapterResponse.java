package com.backend.application.dto.response.chapter;

import com.backend.domain.valueobject.ChapterStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CustomerChapterResponse {
    private Long id;
    private String title;
    private Integer chapterOrder;
    private Integer duration;
    private String audioUrl;
    private ChapterStatus status;
    /** Ảnh và bảng của chương theo thứ tự xuất hiện; rỗng khi chương không có. */
    private List<ChapterAttachmentResponse> attachments;
}
