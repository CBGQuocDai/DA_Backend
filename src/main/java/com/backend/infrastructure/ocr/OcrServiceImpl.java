package com.backend.infrastructure.ocr;

import com.backend.domain.adapter.repository.BookRepository;
import com.backend.domain.adapter.repository.ChapterRepository;
import com.backend.domain.model.Book;
import com.backend.domain.model.Chapter;
import com.backend.domain.valueobject.BookStatus;
import com.backend.domain.valueobject.ChapterStatus;
import com.google.cloud.documentai.v1.*;
import com.google.cloud.documentai.v1.Document.DocumentLayout.DocumentLayoutBlock;
import com.backend.domain.adapter.ocr.OcrService;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcrServiceImpl implements OcrService {

    private static final int MAX_PAGES_PER_CHUNK = 30;
    private static final String HEADING_PREFIX = "heading-";
    private static final String PAGE_BREAK = "\n-------------page_break------------------\n";
    private static final String DEFAULT_TITLE = "Mở đầu";
    private static final int MAX_TITLE_LENGTH = 255;

    @Value("${gcp.documentai.processor-id}")
    private String processorId;

    @Value("${gcp.documentai.project-id}")
    private String projectId;

    @Value("${gcp.documentai.location:us}")
    private String location;

    private DocumentProcessorServiceClient client;

    private final BookRepository bookRepository;

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
    public void processDocumentAsync(Book book, MultipartFile file) {
        try {
            List<Chapter> chapters = extractChapters(file);
            if (chapters.isEmpty()) {
                throw new IllegalStateException("No chapters extracted from document");
            }

            chapterRepository.saveAll(chapters, book);
            log.info("Saved {} chapters for book {}", chapters.size(), book.getId());
            updateStatus(book, BookStatus.UNPUBLISHED);
        } catch (Exception e) {
            log.error("Error processing document for book: {}", book.getId(), e);
            updateStatus(book, BookStatus.FAILED);
        }
    }

    private List<Chapter> extractChapters(MultipartFile file) throws IOException {
        List<List<DocumentLayoutBlock>> chunks = new ArrayList<>();

        try (PDDocument fullDocument = Loader.loadPDF(file.getBytes())) {
            int totalPages = fullDocument.getNumberOfPages();
            log.info("Processing PDF with {} pages, chunk size: {}", totalPages, MAX_PAGES_PER_CHUNK);

            for (int chunkStart = 0; chunkStart < totalPages; chunkStart += MAX_PAGES_PER_CHUNK) {
                int chunkEnd = Math.min(chunkStart + MAX_PAGES_PER_CHUNK, totalPages);
                log.info("Processing chunk: pages {}-{}", chunkStart + 1, chunkEnd);

                chunks.add(flattenBlocks(processChunk(fullDocument, chunkStart, chunkEnd)));
            }
        }
        return buildChapters(chunks);
    }

    private void updateStatus(Book book, BookStatus status) {
        book.setStatus(status);
        bookRepository.update(book);
    }

    private Document processChunk(PDDocument source, int startPage, int endPage) throws IOException {
        RawDocument rawDocument = RawDocument.newBuilder()
                .setMimeType("application/pdf")
                .setContent(ByteString.copyFrom(createChunkPdf(source, startPage, endPage)))
                .build();
        ProcessRequest request = ProcessRequest.newBuilder()
                .setName(getProcessorName())
                .setRawDocument(rawDocument)
                .build();
        return client.processDocument(request).getDocument();
    }

    private byte[] createChunkPdf(PDDocument source, int startPage, int endPage) throws IOException {
        try (PDDocument chunkDoc = new PDDocument()) {
            for (int i = startPage; i < endPage; i++) {
                chunkDoc.addPage(source.getPage(i));
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            chunkDoc.save(baos);
            return baos.toByteArray();
        }
    }

    private static List<DocumentLayoutBlock> flattenBlocks(Document document) {
        List<DocumentLayoutBlock> blocks = new ArrayList<>();
        document.getDocumentLayout().getBlocksList().forEach(block -> collectTextBlocks(block, blocks));
        return blocks;
    }

    private static void collectTextBlocks(DocumentLayoutBlock block, List<DocumentLayoutBlock> out) {
        if (block.hasTextBlock()) {
            out.add(block);
            block.getTextBlock().getBlocksList().forEach(child -> collectTextBlocks(child, out));
        } else if (block.hasListBlock()) {
            block.getListBlock().getListEntriesList()
                    .forEach(entry -> entry.getBlocksList().forEach(child -> collectTextBlocks(child, out)));
        }
    }

    private static List<Chapter> buildChapters(List<List<DocumentLayoutBlock>> chunks) {
        List<Chapter> chapters = new ArrayList<>();
        List<String> titleParts = new ArrayList<>();
        StringBuilder body = new StringBuilder();

        for (List<DocumentLayoutBlock> chunk : chunks) {
            // Page numbers restart at 1 in every OCR chunk.
            int currentPage = 0;
            boolean pageHasBody = false;
            boolean pageHasHeading = false;

            for (DocumentLayoutBlock block : chunk) {
                int page = block.getPageSpan().getPageStart();
                if (page > currentPage) {
                    currentPage = page;
                    pageHasBody = false;
                    pageHasHeading = false;
                }

                String type = block.getTextBlock().getType().toLowerCase();
                String text = block.getTextBlock().getText().trim();

                if ("header".equals(type)) {
                    continue;
                }
                if ("footer".equals(type)) {
                    body.append(PAGE_BREAK);

                    continue;
                }
                if (text.isEmpty()) {
                    continue;
                }

                if (type.startsWith(HEADING_PREFIX) && !pageHasBody) {
                    if (!pageHasHeading) {
                        addChapter(chapters, titleParts, body, false);
                        pageHasHeading = true;
                    }
                    titleParts.add(text);
                } else {
                    pageHasBody = true;
                    body.append(text).append('\n');
                }
            }
        }
        addChapter(chapters, titleParts, body, true);
        return chapters;
    }

    private static void addChapter(List<Chapter> chapters, List<String> titleParts, StringBuilder body,
                                   boolean last) {
        String rawText = body.toString().replace(PAGE_BREAK, "").isBlank() ? "" : body.toString().trim();
        if (rawText.isEmpty() && (!last || titleParts.isEmpty())) {
            body.setLength(0);
            return;
        }

        String title = titleParts.isEmpty() ? DEFAULT_TITLE : String.join(" - ", titleParts);
        if (title.length() > MAX_TITLE_LENGTH) {
            title = title.substring(0, MAX_TITLE_LENGTH);
        }

        chapters.add(Chapter.builder()
                .title(title)
                .chapterOrder(chapters.size())
                .rawText(rawText.isEmpty() ? title : rawText)
                .status(ChapterStatus.TEXT_READY)
                .build());

        titleParts.clear();
        body.setLength(0);
    }
}
