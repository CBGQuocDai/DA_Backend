package com.backend.infrastructure.ocr;

record TextItem(int page, String type, String text) implements Item {
}
