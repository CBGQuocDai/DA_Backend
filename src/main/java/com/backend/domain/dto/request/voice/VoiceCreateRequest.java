package com.backend.domain.dto.request.voice;


import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceCreateRequest {
    private String name;
    private String description;
}
