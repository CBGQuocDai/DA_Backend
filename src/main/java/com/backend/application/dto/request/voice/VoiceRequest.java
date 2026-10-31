package com.backend.application.dto.request.voice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Body of both POST /voice and PUT /voice/{id} (full replacement). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceRequest {
    @NotBlank(message = "Tên giọng không được để trống")
    @Size(max = 255, message = "Tên giọng không được dài quá 255 kí tự")
    private String name;

    @Size(max = 255, message = "Mô tả giọng không được dài quá 255 kí tự")
    private String description;

    /** Temporary path returned by the upload sign URL endpoint (type AUDIO). */
    @NotBlank(message = "File giọng mẫu không được để trống")
    private String exampleAudio;
}
