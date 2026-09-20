package com.backend.domain.adapter.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageService {
    void store(MultipartFile file, String filePath) throws IOException;
    Byte[] load(String path);
}
