package com.backend.infrastructure.ocr;

import com.backend.domain.adapter.repository.ChapterRepository;
import com.backend.domain.model.Book;
import com.backend.domain.model.Chapter;
import com.backend.domain.valueobject.ChapterStatus;
import com.google.cloud.documentai.v1.*;
import com.backend.domain.adapter.ocr.OcrService;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.JsonFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class OcrServiceImpl implements OcrService {

    private static final int MAX_PAGES_PER_REQUEST = 15;

    @Value("${gcp.documentai.processor-id}")
    private String processorId;

    @Value("${gcp.documentai.project-id}")
    private String projectId;

    @Value("${gcp.documentai.location:us}")
    private String location;

    private DocumentProcessorServiceClient client;

    private final ChapterRepository chapterRepository;

    @PostConstruct
    public void init() {
        String endpoint = String.format("%s-documentai.googleapis.com:443", location);
        try {
            DocumentProcessorServiceSettings settings = DocumentProcessorServiceSettings.newBuilder()
                    .setEndpoint(endpoint)
                    .build();
            this.client = DocumentProcessorServiceClient.create(settings);
            log.info("DocumentProcessorServiceClient initialized successfully");
        } catch (IOException e) {
            log.error("Failed to initialize DocumentProcessorServiceClient", e);
            throw new RuntimeException("Failed to initialize DocumentProcessorServiceClient", e);
        }
    }

    private String getProcessorName() {
        return String.format("projects/%s/locations/%s/processors/%s", projectId, location, processorId);
    }

    @Override
    @Async
    public void processDocumentAsync(Book b, MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            ByteString byteString = ByteString.readFrom(inputStream);
            RawDocument rawDocument = RawDocument.newBuilder()
                    .setMimeType("application/pdf")
                    .setContent(byteString)
                    .build();
            ProcessRequest request = ProcessRequest.newBuilder()
                    .setName(getProcessorName())
                    .setRawDocument(rawDocument)
                    .build();
            ProcessResponse response = client.processDocument(request);
            Document document = response.getDocument();

            String jsonOutputName = "output_debug_book_" + (b != null ? b.getId() : "test") + ".json";
            saveDocumentToJson(document, jsonOutputName);
            List<Chapter> chapters = new ArrayList<>();
            Document.DocumentLayout documentLayout =  document.getDocumentLayout();

            int m = documentLayout.getBlocksCount();
            int chapterOrder =0;
            for (int i = 0; i < m; i++) {
                Document.DocumentLayout.DocumentLayoutBlock blocks =  documentLayout.getBlocks(i);
                Chapter temp = new Chapter();
                Document.DocumentLayout.DocumentLayoutBlock.LayoutTextBlock textBlock = blocks.getTextBlock();
                temp.setChapterOrder(++chapterOrder);
                temp.setTitle(textBlock.getText());
                temp.setRawText(textBlock.getBlocks(0).getTextBlock().getText());
                temp.setStatus(ChapterStatus.TEXT_READY);
                temp.setBook(b);
                chapters.add(temp);
            }
            chapterRepository.saveAll(chapters);
        } catch (IOException e) {
            log.error("Error processing document for book: {}", b != null ? b.getId() : "null", e);
            throw new RuntimeException(e);
        }
    }

    public static void saveDocumentToJson(Document document, String outputFilePath) {
        try {
            Files.createDirectories(Paths.get(outputFilePath).getParent() != null ?
                    Paths.get(outputFilePath).getParent() : Paths.get("."));

            String jsonString = JsonFormat.printer()
                    .includingDefaultValueFields()
                    .print(document);

            try (FileWriter fileWriter = new FileWriter(outputFilePath)) {
                fileWriter.write(jsonString);
            }

            log.info("Saved Document JSON to: {}", outputFilePath);

        } catch (IOException e) {
            log.error("Error saving Document to JSON: {}", e.getMessage(), e);
        }
    }
}
