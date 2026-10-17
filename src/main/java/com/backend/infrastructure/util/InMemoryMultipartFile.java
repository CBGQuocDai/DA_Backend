package com.backend.infrastructure.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;


public class InMemoryMultipartFile implements MultipartFile {

    private final byte[] bytes;
    private final String name;
    private final String contentType;

    public InMemoryMultipartFile(byte[] bytes, String name, String contentType) {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes must not be null");
        }
        this.bytes = bytes;
        this.name = name;
        this.contentType = contentType;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getOriginalFilename() {
        return this.name;
    }

    @Override
    public String getContentType() {
        return this.contentType;
    }

    @Override
    public boolean isEmpty() {
        return this.bytes.length == 0;
    }

    @Override
    public long getSize() {
        return this.bytes.length;
    }

    @Override
    public byte[] getBytes() {
        return this.bytes;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(this.bytes);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        throw new UnsupportedOperationException("transferTo is not supported for in-memory files");
    }
}
