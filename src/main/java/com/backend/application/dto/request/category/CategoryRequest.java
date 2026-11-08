package com.backend.application.dto.request.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Body of both POST /categories and PUT /categories/{id} (full replacement). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {
    @NotBlank(message = "Tên thể loại không được để trống")
    @Size(max = 255, message = "Tên thể loại không được dài quá 255 kí tự")
    private String name;

    @Size(max = 255, message = "Mô tả thể loại không được dài quá 255 kí tự")
    private String description;
}
