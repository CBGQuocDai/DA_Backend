package com.backend.application.dto.response.file;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileResponse {
    /** Storage path: send it back in an update request to keep this file. */
    private String path;
    /** Signed URL to read the file. */
    private String url;
}
