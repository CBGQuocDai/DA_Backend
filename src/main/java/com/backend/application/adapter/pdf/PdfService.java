package com.backend.application.adapter.pdf;

/** Port tạo file PDF; cách dàn trang và font do adapter quyết định. */
public interface PdfService {

    /** Tài liệu PDF mới, rỗng; bên gọi phải đóng nó. */
    PdfDocument create();
}
