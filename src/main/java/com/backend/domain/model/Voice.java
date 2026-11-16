package com.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Voice {
    private Long id;
    private String name;
    private String exampleAudio;
    /** Lời thoại của file giọng mẫu (tùy chọn); có thì worker truyền làm ref_text cho OmniVoice. */
    private String exampleTranscript;
    private String description;
}
