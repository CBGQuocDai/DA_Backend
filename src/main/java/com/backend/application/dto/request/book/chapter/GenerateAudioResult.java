package com.backend.application.dto.request.book.chapter;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

// dto định nghĩa cấu trúc json khi tts service xử lý thành công và gửi lại.
public class GenerateAudioResult {
    private Long chapterId;
    private boolean success;
    private String audioPath;
    private Integer duration;
    private String error;
}
