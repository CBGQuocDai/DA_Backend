package com.backend.infrastructure.ocr;

import com.backend.application.adapter.realtime.AdminEventPublisher;
import com.backend.application.adapter.repository.BookRepository;
import com.backend.application.adapter.repository.ChapterRepository;
import com.backend.application.adapter.storage.StorageService;
import com.backend.domain.model.Book;
import com.backend.domain.model.Chapter;
import com.backend.domain.valueobject.BookStatus;
import com.google.cloud.documentai.v1.*;
import com.google.cloud.documentai.v1.Document.DocumentLayout.DocumentLayoutBlock;
import com.google.cloud.documentai.v1.Document.DocumentLayout.DocumentLayoutBlock.LayoutImageBlock;
import com.google.cloud.documentai.v1.Document.DocumentLayout.DocumentLayoutBlock.LayoutTableBlock;
import com.google.cloud.documentai.v1.Document.DocumentLayout.DocumentLayoutBlock.LayoutTableRow;
import com.backend.application.adapter.ocr.OcrService;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcrServiceImpl implements OcrService {

    private static final int MAX_PAGES_PER_CHUNK = 30;
    private static final String HEADING_PREFIX = "heading-";
    private static final String DEFAULT_TITLE = "Mở đầu";
    private static final int MAX_TITLE_LENGTH = 255;
    // Ảnh được trả về trong response và, cũng như bảng, kèm mô tả do LLM viết
    private static final ProcessOptions PROCESS_OPTIONS = ProcessOptions.newBuilder()
            .setLayoutConfig(ProcessOptions.LayoutConfig.newBuilder()
                    .setReturnImages(true)
                    .setEnableImageAnnotation(true)
                    .setEnableTableAnnotation(true))
            .build();
    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/png", "png", "image/jpeg", "jpg", "image/webp", "webp", "image/gif", "gif");

    /** Một phần của sách theo thứ tự đọc; số trang bắt đầu lại từ 1 ở mỗi chunk OCR. */
    private sealed interface Item {
        int page();
    }

    private record TextItem(int page, String type, String text) implements Item {
    }

    private record ImageItem(int page, String path, String description) implements Item {
    }

    private record TableItem(int page, List<List<String>> rows, int headerRows, String description)
            implements Item {
    }

    @Value("${gcp.documentai.processor-id}")
    private String processorId;

    @Value("${gcp.documentai.project-id}")
    private String projectId;

    @Value("${gcp.documentai.location:us}")
    private String location;

    private DocumentProcessorServiceClient client;

    private final BookRepository bookRepository;

    private final ChapterRepository chapterRepository;

    private final StorageService storageService;

    private final AdminEventPublisher adminEventPublisher;

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
    public void processDocumentAsync(Book book) {
        // Giữ lại để xoá ảnh đã upload nếu sách lỗi giữa chừng
        List<String> imagePaths = new ArrayList<>();
        try {
            String imageFolder = ChapterDocument.IMAGE_PATH_PREFIX + "books/" + book.getId() + "/";
            List<Chapter> chapters = extractChapters(
                    storageService.load(book.getContentFile()), imageFolder, imagePaths);
            if (chapters.isEmpty()) {
                throw new IllegalStateException("No chapters extracted from document");
            }

            chapters.forEach(chapter -> chapter.setBookId(book.getId()));
            chapterRepository.saveAll(chapters);
            log.info("Saved {} chapters for book {}", chapters.size(), book.getId());
            updateStatus(book, BookStatus.UNPUBLISHED);
        } catch (Exception e) {
            log.error("Error processing document for book: {}", book.getId(), e);
            imagePaths.forEach(this::deleteImage);
            updateStatus(book, BookStatus.FAILED);
        }
    }

    private void deleteImage(String path) {
        try {
            storageService.delete(path);
        } catch (Exception e) {
            log.warn("Could not remove OCR image {}", path, e);
        }
    }

    private List<Chapter> extractChapters(byte[] pdf, String imageFolder, List<String> imagePaths)
            throws IOException {
        List<List<Item>> chunks = new ArrayList<>();

        try (PDDocument fullDocument = Loader.loadPDF(pdf)) {
            int totalPages = fullDocument.getNumberOfPages();
            log.info("Processing PDF with {} pages, chunk size: {}", totalPages, MAX_PAGES_PER_CHUNK);

            for (int chunkStart = 0; chunkStart < totalPages; chunkStart += MAX_PAGES_PER_CHUNK) {
                int chunkEnd = Math.min(chunkStart + MAX_PAGES_PER_CHUNK, totalPages);
                log.info("Processing chunk: pages {}-{}", chunkStart + 1, chunkEnd);

                chunks.add(flattenBlocks(processChunk(fullDocument, chunkStart, chunkEnd), imageFolder, imagePaths));
            }
        }
        return buildChapters(chunks);
    }

    private void updateStatus(Book book, BookStatus status) {
        book.setStatus(status);
        bookRepository.update(book);
        adminEventPublisher.bookStatusChanged(book.getId(), status);
    }

    private Document processChunk(PDDocument source, int startPage, int endPage) throws IOException {
        RawDocument rawDocument = RawDocument.newBuilder()
                .setMimeType("application/pdf")
                .setContent(ByteString.copyFrom(createChunkPdf(source, startPage, endPage)))
                .build();
        ProcessRequest request = ProcessRequest.newBuilder()
                .setName(getProcessorName())
                .setRawDocument(rawDocument)
                .setProcessOptions(PROCESS_OPTIONS)
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

    private List<Item> flattenBlocks(Document document, String imageFolder, List<String> imagePaths) {
        List<Item> items = new ArrayList<>();
        document.getDocumentLayout().getBlocksList()
                .forEach(block -> collectItems(block, document, imageFolder, imagePaths, items));
        return items;
    }

    private void collectItems(DocumentLayoutBlock block, Document document, String imageFolder,
                              List<String> imagePaths, List<Item> out) {
        int page = block.getPageSpan().getPageStart();
        if (block.hasTextBlock()) {
            out.add(new TextItem(page, block.getTextBlock().getType().toLowerCase(),
                    block.getTextBlock().getText().trim()));
            block.getTextBlock().getBlocksList()
                    .forEach(child -> collectItems(child, document, imageFolder, imagePaths, out));
        } else if (block.hasListBlock()) {
            block.getListBlock().getListEntriesList().forEach(entry -> entry.getBlocksList()
                    .forEach(child -> collectItems(child, document, imageFolder, imagePaths, out)));
        } else if (block.hasImageBlock()) {
            LayoutImageBlock image = block.getImageBlock();
            String path = storeImage(image, document, imageFolder, imagePaths);
            if (path != null) {
                out.add(new ImageItem(page, path, image.getAnnotations().getDescription().trim()));
            }
        } else if (block.hasTableBlock()) {
            LayoutTableBlock table = block.getTableBlock();
            List<List<String>> rows = Stream.concat(table.getHeaderRowsList().stream(), table.getBodyRowsList().stream())
                    .map(OcrServiceImpl::cellTexts)
                    .toList();
            if (rows.stream().flatMap(List::stream).anyMatch(cell -> !cell.isEmpty())) {
                out.add(new TableItem(page, rows, table.getHeaderRowsCount(),
                        table.getAnnotations().getDescription().trim()));
            }
        }
    }

    /** Lưu ảnh của block lên storage và trả về đường dẫn, null nếu block không có ảnh. */
    private String storeImage(LayoutImageBlock image, Document document, String imageFolder,
                              List<String> imagePaths) {
        byte[] content = null;
        String mimeType = image.getMimeType();
        if (image.hasBlobAssetId()) {
            Document.BlobAsset asset = document.getBlobAssetsList().stream()
                    .filter(candidate -> candidate.getAssetId().equals(image.getBlobAssetId()))
                    .findFirst().orElse(null);
            if (asset != null) {
                content = asset.getContent().toByteArray();
                if (mimeType.isEmpty()) {
                    mimeType = asset.getMimeType();
                }
            }
        } else if (image.hasDataUri()) {
            // data:<mime type>;base64,<content>
            String dataUri = image.getDataUri();
            int comma = dataUri.indexOf(',');
            if (comma > 0 && dataUri.substring(0, comma).endsWith(";base64")) {
                content = Base64.getDecoder().decode(dataUri.substring(comma + 1));
                if (mimeType.isEmpty()) {
                    mimeType = dataUri.substring("data:".length(), comma - ";base64".length());
                }
            }
        }
        String extension = IMAGE_EXTENSIONS.get(mimeType);
        if (content == null || content.length == 0 || extension == null) {
            log.warn("Skipping OCR image without usable content (mime type '{}')", mimeType);
            return null;
        }
        String path = imageFolder + UUID.randomUUID() + "." + extension;
        storageService.store(content, path, mimeType);
        imagePaths.add(path);
        return path;
    }

    private static List<String> cellTexts(LayoutTableRow row) {
        return row.getCellsList().stream()
                .map(cell -> cell.getBlocksList().stream()
                        .flatMap(OcrServiceImpl::texts)
                        .collect(Collectors.joining(" ")))
                .toList();
    }

    /** Mọi đoạn văn bản không rỗng trong block, theo thứ tự đọc. */
    private static Stream<String> texts(DocumentLayoutBlock block) {
        if (block.hasTextBlock()) {
            return Stream.concat(Stream.of(block.getTextBlock().getText().trim()),
                            block.getTextBlock().getBlocksList().stream().flatMap(OcrServiceImpl::texts))
                    .filter(text -> !text.isEmpty());
        }
        if (block.hasListBlock()) {
            return block.getListBlock().getListEntriesList().stream()
                    .flatMap(entry -> entry.getBlocksList().stream())
                    .flatMap(OcrServiceImpl::texts);
        }
        return Stream.empty();
    }

    private static List<Chapter> buildChapters(List<List<Item>> chunks) {
        List<Chapter> chapters = new ArrayList<>();
        List<String> titleParts = new ArrayList<>();
        ChapterDocument body = new ChapterDocument();

        for (List<Item> chunk : chunks) {
            int currentPage = 0;
            boolean pageHasBody = false;
            boolean pageHasHeading = false;
            boolean pageBroken = false;

            for (Item item : chunk) {
                if (item.page() > currentPage) {
                    if (currentPage > 0 && !pageBroken && body.hasText()) {
                        breakPage(body);
                    }
                    pageBroken = false;
                    currentPage = item.page();
                    pageHasBody = false;
                    pageHasHeading = false;
                }

                switch (item) {
                    // Tệp đính kèm nằm giữa các heading của trang tiêu đề không kết thúc tiêu đề
                    case ImageItem image -> body.image(image.path(), image.description());
                    case TableItem table -> body.table(table.rows(), table.headerRows(), table.description());
                    case TextItem(int page, String type, String text) -> {
                        if ("header".equals(type)) {
                            continue;
                        }
                        if ("footer".equals(type)) {
                            if (!pageBroken) {
                                breakPage(body);
                                pageBroken = true;
                            }
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
                            body.paragraph(text);
                        }
                    }
                }
            }
            if (currentPage > 0 && !pageBroken && body.hasText()) {
                breakPage(body);
            }
        }
        addChapter(chapters, titleParts, body, true);
        return chapters;
    }

    private static void breakPage(ChapterDocument body) {
        body.paragraph("");
        body.pageBreak();
    }

    private static void addChapter(List<Chapter> chapters, List<String> titleParts, ChapterDocument body,
                                   boolean last) {
        if (!body.hasText() && !last) {
            // Tệp đính kèm không có văn bản đi cùng (hoa văn ở trang tiêu đề) được đưa sang đầu chương kế tiếp
            if (!body.hasAttachment()) {
                body.clear();
            }
            return;
        }
        String rawText = body.hasText() || body.hasAttachment() ? body.text() : "";
        if (rawText.isEmpty() && titleParts.isEmpty()) {
            body.clear();
            return;
        }

        String title = titleParts.isEmpty() ? DEFAULT_TITLE : String.join(" - ", titleParts);
        if (title.length() > MAX_TITLE_LENGTH) {
            title = title.substring(0, MAX_TITLE_LENGTH);
        }

        chapters.add(Chapter.builder()
                .title(title)
                .readingSpeed(1D)
                .chapterOrder(chapters.size())
                .rawText(rawText.isEmpty() ? title : rawText)
                .contentJson(body.json())
                .build());

        titleParts.clear();
        body.clear();
    }
}
