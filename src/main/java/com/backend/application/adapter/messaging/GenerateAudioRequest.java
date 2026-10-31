package com.backend.application.adapter.messaging;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateAudioRequest {
    private Long chapterId;
    private String exampleVoiceUrl;
    private String text;
    private Double speed;
}
