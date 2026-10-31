package com.backend.application.service;

import com.backend.application.mapper.ResponseMapper;
import com.backend.application.service.impl.BookServiceImpl;
import com.backend.application.adapter.ocr.OcrService;
import com.backend.application.adapter.repository.BookRepository;
import com.backend.application.adapter.storage.StorageService;
import com.backend.application.dto.request.book.BookRequest;
import com.backend.application.dto.response.PageResponse;
import com.backend.application.dto.response.book.BookResponse;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Book;
import com.backend.domain.model.BookStat;
import com.backend.domain.valueobject.BusinessError;
import com.backend.domain.valueobject.UploadType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    private static final String TEMP_CONTENT = "tmp/pdfs/moi.pdf";
    private static final String TEMP_COVER = "tmp/images/moi.png";

    @Mock
    private BookRepository bookRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private OcrService ocrService;
    @Mock
    private FileManagementService fileManagementService;

    private BookServiceImpl bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookServiceImpl(bookRepository, storageService, ocrService, fileManagementService,
                new ResponseMapper(storageService));
    }

    private static BookRequest request(String contentFile, String coverImage) {
        return BookRequest.builder()
                .title("Sách").price(BigDecimal.TEN).description("Mô tả")
                .voiceId(1L).categoryIds(List.of(1L)).authorIds(List.of(2L))
                .contentFile(contentFile).coverImage(coverImage)
                .build();
    }

    private void givenExistingBook() {
        Book existing = new Book();
        existing.setContentFile("pdfs/cu.pdf");
        existing.setCoverImage("images/cu.png");
        // The second read is the reload that signs URLs on the returned copy
        when(bookRepository.findById(7L)).thenReturn(Optional.of(existing), Optional.of(new Book()));
    }

    private Book updatedBook() {
        ArgumentCaptor<Book> saved = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).update(saved.capture());
        return saved.getValue();
    }

    @Test
    void getBookById_returnsThePathAndSignedUrlOfEachFile() {
        Book book = new Book();
        book.setContentFile("pdfs/a.pdf");
        when(bookRepository.findById(7L)).thenReturn(Optional.of(book));
        when(storageService.createSignUrl("pdfs/a.pdf", 60L)).thenReturn("https://signed");

        BookResponse response = bookService.getBookById(7L);

        assertEquals("pdfs/a.pdf", response.getContentFile().getPath());
        assertEquals("https://signed", response.getContentFile().getUrl());
        assertNull(response.getCoverImage());
    }

    @Test
    void getBooks_keepsThePageAndTheRatings() {
        BookStat stat = BookStat.builder().id(1L).coverImage("images/a.png").averageRating(4.5).ratingCount(2L).build();
        when(bookRepository.findAllWithStats(null, null, null, null, 0, 10)).thenReturn(
                PageResponse.<BookStat>builder().content(List.of(stat)).total(1).totalPage(1).page(0).pageSize(10).build());

        PageResponse<BookResponse> response = bookService.getBooks(null, null, null, null, 0, 10);

        assertEquals(1, response.getTotal());
        assertEquals("images/a.png", response.getContent().get(0).getCoverImage().getPath());
        assertEquals(4.5, response.getContent().get(0).getAverageRating());
        assertEquals(2L, response.getContent().get(0).getRatingCount());
    }

    @Test
    void createBook_verifiesBeforeSavingAndMovesBeforeOcr() {
        when(bookRepository.insert(any())).then(returnsFirstArg());

        BookResponse created = bookService.createBook(request(TEMP_CONTENT, TEMP_COVER));

        assertEquals("pdfs/moi.pdf", created.getContentFile().getPath());
        assertEquals("images/moi.png", created.getCoverImage().getPath());
        InOrder order = inOrder(fileManagementService, bookRepository, ocrService);
        order.verify(fileManagementService).verifyUpload(TEMP_CONTENT, UploadType.PDF);
        order.verify(fileManagementService).verifyUpload(TEMP_COVER, UploadType.IMAGE);
        order.verify(bookRepository).insert(any());
        order.verify(fileManagementService).moveToPermanent(TEMP_CONTENT, UploadType.PDF);
        order.verify(fileManagementService).moveToPermanent(TEMP_COVER, UploadType.IMAGE);
        order.verify(ocrService).processDocumentAsync(any());
    }

    @Test
    void createBook_savesAndMovesNothingWhenAFileIsRejected() {
        // Lenient: the PDF is verified first, with other arguments
        lenient().doThrow(new BusinessException(BusinessError.IMAGE_FORMAT_INCORRECT))
                .when(fileManagementService).verifyUpload(TEMP_COVER, UploadType.IMAGE);

        assertThrows(BusinessException.class, () -> bookService.createBook(request(TEMP_CONTENT, TEMP_COVER)));

        verify(bookRepository, never()).insert(any());
        verify(fileManagementService, never()).moveToPermanent(anyString(), any());
        verifyNoInteractions(ocrService);
    }

    @Test
    void createBook_doesNotMoveFilesWhenTheSaveFails() {
        when(bookRepository.insert(any())).thenThrow(new IllegalStateException("db"));

        assertThrows(IllegalStateException.class, () -> bookService.createBook(request(TEMP_CONTENT, TEMP_COVER)));

        verify(fileManagementService, never()).moveToPermanent(anyString(), any());
    }

    @Test
    void updateBook_treatsTheCurrentPathAsNoChange() throws IOException {
        givenExistingBook();

        bookService.updateBook(7L, request("pdfs/cu.pdf", "images/cu.png"));

        assertEquals("pdfs/cu.pdf", updatedBook().getContentFile());
        verifyNoInteractions(fileManagementService);
        verify(storageService, never()).delete(anyString());
    }

    @Test
    void updateBook_replacesOnlyTheNewFile() throws IOException {
        givenExistingBook();

        bookService.updateBook(7L, request("pdfs/cu.pdf", TEMP_COVER));

        assertEquals("pdfs/cu.pdf", updatedBook().getContentFile());
        assertEquals("images/moi.png", updatedBook().getCoverImage());
        verify(fileManagementService).verifyUpload(TEMP_COVER, UploadType.IMAGE);
        verify(fileManagementService).moveToPermanent(TEMP_COVER, UploadType.IMAGE);
        verify(fileManagementService, never()).verifyUpload(anyString(), eq(UploadType.PDF));
        verify(storageService).delete("images/cu.png");
        verify(storageService, never()).delete("pdfs/cu.pdf");
    }

    @Test
    void updateBook_changesNothingWhenTheNewFileIsRejected() throws IOException {
        Book existing = new Book();
        existing.setContentFile("pdfs/cu.pdf");
        existing.setCoverImage("images/cu.png");
        when(bookRepository.findById(7L)).thenReturn(Optional.of(existing));
        doThrow(new BusinessException(BusinessError.UPLOAD_NOT_FOUND))
                .when(fileManagementService).verifyUpload("pdfs/cua-sach-khac.pdf", UploadType.PDF);

        assertThrows(BusinessException.class,
                () -> bookService.updateBook(7L, request("pdfs/cua-sach-khac.pdf", "images/cu.png")));

        verify(bookRepository, never()).update(any());
        verify(storageService, never()).delete(anyString());
    }
}
