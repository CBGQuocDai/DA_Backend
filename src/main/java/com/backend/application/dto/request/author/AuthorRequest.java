package com.backend.application.dto.request.author;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Body of both POST /authors and PUT /authors/{id} (full replacement). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorRequest {
    @NotBlank(message = "Tên tác giả không được để trống")
    @Size(max = 255, message = "Tên tác giả không được dài quá 255 kí tự")
    private String fullName;

    @Size(max = 255, message = "Tiểu sử không được dài quá 255 kí tự")
    private String bio;

    /**
     * Optional. A temporary path from the upload sign URL endpoint (type IMAGE) for a new
     * avatar, the current path to keep it, or empty to remove it.
     */
    private String avatar;
}
