package com.backend.infrastructure.ocr;

import java.util.List;

record TableItem(int page, List<List<String>> rows, int headerRows, String description) implements Item {
}
