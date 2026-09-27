package com.backend.domain.adapter.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageService {
//    viết lại lưu theo bất đồng bộ
    void store(MultipartFile file, String filePath);
    String createSignUrl(String filePath, Long minutes);
    Byte[] load(String path);
}
