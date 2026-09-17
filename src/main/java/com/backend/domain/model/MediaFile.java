package com.backend.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MediaFile {
    private Long id;
    private String fileName;
    private String type;
    private String path;
}
