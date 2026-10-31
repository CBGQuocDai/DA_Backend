package com.backend.application.adapter.file;

public interface FileDetector {

    String detectContentType(byte[] content);
}
