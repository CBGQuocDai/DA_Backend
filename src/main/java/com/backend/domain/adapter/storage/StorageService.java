package com.backend.domain.adapter.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageService {
    void store(MultipartFile file, String filePath);
    String createSignUrl(String filePath, Long minutes);
    Byte[] load(String path);

    /**
     * Xóa 1 file khỏi storage. Ném {@link IOException} nếu lỗi (ví dụ: file
     * không tồn tại, mất quyền, mạng...).
     */
    void delete(String filePath) throws IOException;
}
