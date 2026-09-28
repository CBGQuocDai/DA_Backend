package com.backend.infrastructure.adapter.ocr;

import com.backend.domain.adapter.ocr.OcrService;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcrServiceImpl implements OcrService {

    private static final int MAX_PAGES_PER_REQUEST = 15;

    @Value("${gcp.documentai.processor-id:}")
    private String processorId;

    @Value("${gcp.documentai.project-id:}")
    private String projectId;

    @Value("${gcp.documentai.location:us}")
    private String location;

    @Value("${gcs.bucket:}")
    private String bucket;


    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private GoogleCredentials googleCredentials;


    @Override
    @Async
    public void processDocumentAsync(Long bookId, String pdfPath) {

    }
}
