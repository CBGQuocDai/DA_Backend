package com.backend.application.dto.response.chapter;

import com.backend.application.adapter.content.ChapterAttachment;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** Ảnh hoặc bảng của chương mà người nghe mở xem trong mục tệp đính kèm. */
@Getter
@Builder
public class ChapterAttachmentResponse {
    private ChapterAttachment.Type type;
    /** Nhãn hiển thị, đánh số riêng theo loại trong chương: "Hình 1", "Bảng 1". */
    private String label;
    /** URL đã ký, chỉ có với ảnh. */
    private String imageUrl;
    /** Các hàng của bảng, chỉ có với bảng; {@code headerRows} hàng đầu là tiêu đề. */
    private List<List<String>> rows;
    private Integer headerRows;
}
