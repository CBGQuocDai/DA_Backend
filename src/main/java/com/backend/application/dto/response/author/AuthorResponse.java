package com.backend.application.dto.response.author;

import com.backend.application.dto.response.file.FileResponse;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorResponse {
    private Long id;
    private String fullName;
    private FileResponse avatar;
    private String bio;
}
