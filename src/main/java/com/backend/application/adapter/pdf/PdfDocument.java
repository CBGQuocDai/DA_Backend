package com.backend.application.adapter.pdf;

import java.io.Closeable;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Tài liệu PDF đang viết dở: nội dung chảy từ trên xuống, tự sang trang khi hết chỗ.
 * Mọi thao tác ném {@link UncheckedIOException} khi không viết được.
 */
public interface PdfDocument extends Closeable {

    void title(String text);

    /** Tiêu đề một mục, có khoảng trống phía trên để tách với mục trước. */
    void heading(String text);

    void paragraph(String text);

    /**
     * Ảnh co lại cho vừa trang, không phóng to.
     *
     * @throws IllegalArgumentException hoặc {@link UncheckedIOException} nếu không đọc được định dạng ảnh
     */
    void image(byte[] content, String name);

    /** Bảng với {@code headerRows} hàng đầu là hàng tiêu đề. */
    void table(List<List<String>> rows, int headerRows);

    /** Nội dung file PDF; sau lời gọi này không viết thêm được nữa. */
    byte[] toByteArray();

    @Override
    void close();
}
