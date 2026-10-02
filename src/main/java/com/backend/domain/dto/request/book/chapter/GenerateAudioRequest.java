package com.backend.domain.dto.request.book.chapter;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateAudioRequest {
    private String exampleVoiceUrl;
    private String text;
}
