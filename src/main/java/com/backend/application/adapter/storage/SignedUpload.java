package com.backend.application.adapter.storage;

import java.util.Map;

public record SignedUpload(String url, Map<String, String> headers) {
}
