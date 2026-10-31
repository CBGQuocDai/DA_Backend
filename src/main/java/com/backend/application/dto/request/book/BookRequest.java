package com.backend.application.dto.request.book;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/** Body of both POST /books and PUT /books/{id} (full replacement). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookRequest {
    @NotBlank(message = "Tiêu đề sách không được để trống")
    @Size(max = 255, message = "Tiêu đề sách không được dài quá 255 kí tự")
    private String title;

    @NotNull(message = "Giá sách không được để trống")
    @PositiveOrZero(message = "Giá sách không hợp lệ (phải lớn hơn hoặc bằng 0)")
    private BigDecimal price;

    @NotBlank(message = "Mô tả sách không được để trống")
    @Size(max = 2000, message = "Mô tả sách không được dài quá 2000 kí tự")
    private String description;

    @NotNull(message = "Giọng đọc của sách không được để trống")
    private Long voiceId;

    @NotEmpty(message = "Sách phải thuộc ít nhất một danh mục")
    private List<@NotNull(message = "Danh mục không hợp lệ") Long> categoryIds;

    @NotEmpty(message = "Sách phải có ít nhất một tác giả")
    private List<@NotNull(message = "Tác giả không hợp lệ") Long> authorIds;

    /** Temporary path returned by the upload sign URL endpoint (type PDF). */
    @NotBlank(message = "File PDF của sách không được để trống")
    private String contentFile;

    /** Temporary path returned by the upload sign URL endpoint (type IMAGE). */
    @NotBlank(message = "Ảnh bìa của sách không được để trống")
    private String coverImage;
}
