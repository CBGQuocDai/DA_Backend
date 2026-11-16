package com.backend.infrastructure.ocr;

record ImageItem(int page, String path, String description) implements Item {
}
