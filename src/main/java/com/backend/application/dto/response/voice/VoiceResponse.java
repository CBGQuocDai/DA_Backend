package com.backend.application.dto.response.voice;

import com.backend.application.dto.response.file.FileResponse;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceResponse {
    private Long id;
    private String name;
    private String description;
    private FileResponse exampleAudio;
}
