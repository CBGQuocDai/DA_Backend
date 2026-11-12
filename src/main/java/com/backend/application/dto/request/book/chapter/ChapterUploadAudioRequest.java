package com.backend.application.dto.request.book.chapter;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChapterUploadAudioRequest {
    @NotBlank
    private String audioPath;
    @NotNull
    @Min(1)
    private Integer duration;
}
