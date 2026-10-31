package com.backend.application.dto.response.upload;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadUrlResponse {
    /** Pre-signed URL the client PUTs the file bytes to. */
    private String uploadUrl;
    /** Temporary storage path to send in the book/voice request once the upload succeeds. */
    private String filePath;
    /** Headers the PUT request must carry exactly as given. */
    private Map<String, String> headers;
}
