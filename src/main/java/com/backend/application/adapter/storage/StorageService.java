package com.backend.application.adapter.storage;

import java.io.IOException;
import java.util.Optional;

public interface StorageService {
    void store(byte[] content, String filePath, String contentType);
    String createSignUrl(String filePath, Long minutes);

    /**
     * Tạo URL ký sẵn để client PUT file thẳng lên storage. Chữ ký gắn với
     * {@code contentType} và giới hạn dung lượng {@code maxSize}, nên client
     * không thể upload loại file hoặc kích thước khác.
     */
    SignedUpload createUploadSignUrl(String filePath, String contentType, long maxSize, long minutes);

    byte[] load(String filePath);

    /** Chỉ đọc tối đa {@code maxBytes} byte đầu file, không tải cả file về. */
    byte[] loadHead(String filePath, int maxBytes);

    /** Dung lượng thật của file trên storage, rỗng nếu file không tồn tại. */
    Optional<Long> findSize(String filePath);

    /** Chuyển file sang đường dẫn khác trong cùng bucket. */
    void move(String sourcePath, String targetPath);

    /**
     * Xóa 1 file khỏi storage. Ném {@link IOException} nếu lỗi (ví dụ: file
     * không tồn tại, mất quyền, mạng...).
     */
    void delete(String filePath) throws IOException;
}
