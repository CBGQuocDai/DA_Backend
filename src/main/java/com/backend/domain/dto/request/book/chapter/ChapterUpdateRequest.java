package com.backend.domain.dto.request.book.chapter;


import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterUpdateRequest {
    private String title;
    private String rawText;
}
