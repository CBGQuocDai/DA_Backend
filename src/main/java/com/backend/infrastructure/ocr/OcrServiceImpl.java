package com.backend.infrastructure.ocr;

import com.backend.application.adapter.ocr.OcrBlock;
import com.backend.application.adapter.ocr.OcrBlock.TextKind;
import com.backend.application.adapter.ocr.OcrService;
import com.google.cloud.documentai.v1.*;
import com.google.cloud.documentai.v1.Document.DocumentLayout.DocumentLayoutBlock;
import com.google.cloud.documentai.v1.Document.DocumentLayout.DocumentLayoutBlock.LayoutImageBlock;
import com.google.cloud.documentai.v1.Document.DocumentLayout.DocumentLayoutBlock.LayoutTableBlock;
import com.google.cloud.documentai.v1.Document.DocumentLayout.DocumentLayoutBlock.LayoutTableRow;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Nhận dạng PDF bằng Document AI (layout parser), chia file thành từng đoạn để không vượt giới hạn số trang. */
@Slf4j
@Service
public class OcrServiceImpl implements OcrService {

    private static final int MAX_PAGES_PER_CHUNK = 30;
    private static final String HEADING_PREFIX = "heading-";
    // Ảnh được trả về trong response và, cũng như bảng, kèm mô tả do LLM viết
    private static final ProcessOptions PROCESS_OPTIONS = ProcessOptions.newBuilder()
            .setLayoutConfig(ProcessOptions.LayoutConfig.newBuilder()
                    .setReturnImages(true)
                    .setEnableImageAnnotation(true)
                    .setEnableTableAnnotation(true))
            .build();

    @Value("${gcp.documentai.processor-id}")
    private String processorId;

    @Value("${gcp.documentai.project-id}")
    private String projectId;

    @Value("${gcp.documentai.location:us}")
    private String location;

    private DocumentProcessorServiceClient client;

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
    public List<OcrBlock> recognize(byte[] pdf) {
        List<OcrBlock> blocks = new ArrayList<>();
        try (PDDocument fullDocument = Loader.loadPDF(pdf)) {
            int totalPages = fullDocument.getNumberOfPages();
            log.info("Processing PDF with {} pages, chunk size: {}", totalPages, MAX_PAGES_PER_CHUNK);

            for (int chunkStart = 0; chunkStart < totalPages; chunkStart += MAX_PAGES_PER_CHUNK) {
                int chunkEnd = Math.min(chunkStart + MAX_PAGES_PER_CHUNK, totalPages);
                log.info("Processing chunk: pages {}-{}", chunkStart + 1, chunkEnd);

                Document document = processChunk(fullDocument, chunkStart, chunkEnd);
                // Số trang Document AI trả về tính trong từng đoạn; cộng thêm để thành số trang trên cả file
                int pageOffset = chunkStart;
                document.getDocumentLayout().getBlocksList()
                        .forEach(block -> collectBlocks(block, document, pageOffset, blocks));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read PDF for OCR", e);
        }
        return blocks;
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

    private static void collectBlocks(DocumentLayoutBlock block, Document document, int pageOffset,
                                      List<OcrBlock> out) {
        int page = pageOffset + block.getPageSpan().getPageStart();
        if (block.hasTextBlock()) {
            String type = block.getTextBlock().getType().toLowerCase();
            out.add(new OcrBlock.Text(page, kindOf(type), block.getTextBlock().getText().trim()));
            block.getTextBlock().getBlocksList()
                    .forEach(child -> collectBlocks(child, document, pageOffset, out));
        } else if (block.hasListBlock()) {
            block.getListBlock().getListEntriesList().forEach(entry -> entry.getBlocksList()
                    .forEach(child -> collectBlocks(child, document, pageOffset, out)));
        } else if (block.hasImageBlock()) {
            LayoutImageBlock image = block.getImageBlock();
            OcrBlock.Image result = toImage(page, image, document);
            if (result != null) {
                out.add(result);
            }
        } else if (block.hasTableBlock()) {
            LayoutTableBlock table = block.getTableBlock();
            List<List<String>> rows = Stream.concat(table.getHeaderRowsList().stream(), table.getBodyRowsList().stream())
                    .map(OcrServiceImpl::cellTexts)
                    .toList();
            if (rows.stream().flatMap(List::stream).anyMatch(cell -> !cell.isEmpty())) {
                out.add(new OcrBlock.Table(page, rows, table.getHeaderRowsCount(),
                        table.getAnnotations().getDescription().trim()));
            }
        }
    }

    private static TextKind kindOf(String type) {
        if (type.startsWith(HEADING_PREFIX)) {
            return TextKind.HEADING;
        }
        return switch (type) {
            case "header" -> TextKind.HEADER;
            case "footer" -> TextKind.FOOTER;
            default -> TextKind.BODY;
        };
    }

    /** Nội dung ảnh của block, null nếu block không có ảnh dùng được. */
    private static OcrBlock.Image toImage(int page, LayoutImageBlock image, Document document) {
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
        if (content == null || content.length == 0) {
            log.warn("Skipping OCR image without content (mime type '{}')", mimeType);
            return null;
        }
        return new OcrBlock.Image(page, content, mimeType, image.getAnnotations().getDescription().trim());
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
}
