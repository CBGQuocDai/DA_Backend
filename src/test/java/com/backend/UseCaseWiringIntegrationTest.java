package com.backend;

import com.backend.application.dto.request.book.chapter.GenerateAudioResult;
import com.backend.application.adapter.repository.ChapterRepository;
import com.backend.application.dto.request.book.chapter.ChapterCreateRequest;
import com.backend.application.dto.request.book.chapter.ChapterGenerateAudioRequest;
import com.backend.application.dto.request.book.chapter.ChapterUpdateRequest;
import com.backend.application.dto.request.voice.VoiceRequest;
import com.backend.application.dto.response.BookFavoriteResponse;
import com.backend.application.dto.response.book.BookResponse;
import com.backend.application.dto.response.payment.PaymentResponse;
import com.backend.application.dto.response.chapter.ChapterResponse;
import com.backend.application.adapter.messaging.MessageQueue;
import com.backend.application.adapter.ocr.OcrService;
import com.backend.application.adapter.repository.BookRepository;
import com.backend.application.adapter.repository.CustomerRepository;
import com.backend.application.adapter.repository.VoiceRepository;
import com.backend.application.adapter.storage.StorageService;
import com.backend.application.service.BookFavoriteService;
import com.backend.application.service.ChapterService;
import com.backend.application.service.CustomerBookService;
import com.backend.application.service.FileManagementService;
import com.backend.application.service.PaymentService;
import com.backend.application.service.VoiceService;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Book;
import com.backend.domain.model.Chapter;
import com.backend.domain.model.Customer;
import com.backend.domain.valueobject.BookStatus;
import com.backend.domain.valueobject.BusinessError;
import com.backend.domain.valueobject.UploadType;
import com.backend.domain.valueobject.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Use cases wired for real on H2: transactions, and the payment flow through the persistence adapters. */
@SpringBootTest
@ActiveProfiles("test")
class UseCaseWiringIntegrationTest {

    @MockitoBean
    OcrService ocrService;
    @MockitoBean
    StorageService storageService;
    @MockitoBean
    FileManagementService fileManagementService;
    @MockitoBean
    MessageQueue messageQueue;

    @Autowired
    VoiceService voiceService;
    @Autowired
    VoiceRepository voiceRepository;
    @Autowired
    ChapterService chapterService;
    @Autowired
    ChapterRepository chapterRepository;
    @Autowired
    PaymentService paymentService;
    @Autowired
    CustomerBookService customerBookService;
    @Autowired
    BookFavoriteService bookFavoriteService;
    @Autowired
    BookRepository bookRepository;
    @Autowired
    CustomerRepository customerRepository;
    @Autowired
    JsonMapper jsonMapper;

    @Test
    void useCaseRollsBackWhenItFailsAfterSaving() {
        String tempPath = "tmp/audio/rollback.wav";
        doThrow(new IllegalStateException("gcs")).when(fileManagementService).moveToPermanent(tempPath, UploadType.AUDIO);
        int before = voiceRepository.findAll(0, 100).getTotal();

        assertThrows(IllegalStateException.class, () -> voiceService.createVoice(
                VoiceRequest.builder().name("Giọng").description("Mô tả").exampleAudio(tempPath).build()));

        assertEquals(before, voiceRepository.findAll(0, 100).getTotal());
    }

    @Test
    void useCaseRunsInATransactionSoItCanLockARow() {
        // A pessimistic lock fails outright without a transaction, even when no row matches
        assertDoesNotThrow(() -> chapterService.handleAudioResult(
                GenerateAudioResult.builder().chapterId(-1L).success(true).audioPath("x.wav").build()));
    }

    @Test
    void chapterTextAndFormattingStaySyncedInTheDatabase() {
        Book book = new Book();
        book.setTitle("Sách đồng bộ");
        book.setPrice(BigDecimal.TEN);
        book.setDescription("Mô tả");
        book.setCoverImage("images/a.png");
        book.setContentFile("pdfs/a.pdf");
        book.setStatus(BookStatus.PUBLISHED);
        book = bookRepository.insert(book);
        Long chapterId = chapterRepository.saveAll(List.of(Chapter.builder()
                .title("Chương").chapterOrder(0).rawText("Cũ").build()), book).get(0).getId();
        String doc = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"center\"},"
                + "\"content\":[{\"type\":\"text\",\"text\":\"Giữa\"}]},{\"type\":\"pageBreak\"}]}";

        chapterService.update(book.getId(), chapterId, ChapterUpdateRequest.builder().contentJson(doc).build());
        Chapter formatted = chapterRepository.findById(chapterId).orElseThrow();
        assertEquals(doc, formatted.getContentJson());
        assertEquals("Giữa\n" + Chapter.PAGE_BREAK, formatted.getRawText());

        chapterService.update(book.getId(), chapterId, ChapterUpdateRequest.builder().title("Tên mới").build());
        assertEquals(doc, chapterRepository.findById(chapterId).orElseThrow().getContentJson());

        chapterService.update(book.getId(), chapterId, ChapterUpdateRequest.builder().rawText("Thuần").build());
        Chapter plain = chapterRepository.findById(chapterId).orElseThrow();
        assertEquals("Thuần", plain.getRawText());
        assertNull(plain.getContentJson());
    }

    @Test
    void newChapterStartsWithEmptyContent() {
        Long bookId = newBook().getId();

        ChapterResponse created = chapterService.createChapter(bookId, createRequest("  Mở đầu  ", 1));

        Chapter stored = chapterRepository.findById(created.getId()).orElseThrow();
        assertEquals("Mở đầu", stored.getTitle());
        assertEquals(1, stored.getChapterOrder());
        assertEquals("", stored.getRawText());
        assertNull(stored.getContentJson());
        assertNull(stored.getAudioUrl());
        assertEquals(1, created.getChapterOrder());
    }

    @Test
    void creatingOnATakenOrderPushesLaterChaptersBack() {
        Book book = newBook();
        seedChapters(book, 1, 2, 3);

        chapterService.createChapter(book.getId(), createRequest("Mới", 2));

        assertEquals(List.of("1:C1", "2:Mới", "3:C2", "4:C3"), chapterOrder(book));
    }

    @Test
    void creatingOnAFreeOrderLeavesOtherChaptersAlone() {
        Book book = newBook();
        seedChapters(book, 1, 2, 4);

        chapterService.createChapter(book.getId(), createRequest("Mới", 3));

        assertEquals(List.of("1:C1", "2:C2", "3:Mới", "4:C4"), chapterOrder(book));
    }

    @Test
    void creatingBeyondTheEndAppendsWithoutAGap() {
        Book book = newBook();
        seedChapters(book, 1, 2, 3);

        chapterService.createChapter(book.getId(), createRequest("Mới", 99));

        assertEquals(List.of("1:C1", "2:C2", "3:C3", "4:Mới"), chapterOrder(book));
    }

    @Test
    void creatingBeyondTheEndOfAnEmptyBookStartsAtOne() {
        Book book = newBook();

        chapterService.createChapter(book.getId(), createRequest("Mới", 99));

        assertEquals(List.of("1:Mới"), chapterOrder(book));
    }

    @Test
    void creatingOnOrderZeroKeepsAnOcrBookUnique() {
        Book book = newBook();
        seedChapters(book, 0, 1);

        chapterService.createChapter(book.getId(), createRequest("Mới", 0));

        assertEquals(List.of("0:Mới", "1:C0", "2:C1"), chapterOrder(book));
    }

    @Test
    void creatingLeavesOtherBooksUntouched() {
        Book book = newBook();
        Book other = newBook();
        seedChapters(book, 1, 2);
        seedChapters(other, 1, 2);

        chapterService.createChapter(book.getId(), createRequest("Mới", 1));

        assertEquals(List.of("1:C1", "2:C2"), chapterOrder(other));
    }

    @Test
    void creatingInAMissingBookIsRejected() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> chapterService.createChapter(-1L, createRequest("Mới", 1)));

        assertEquals(BusinessError.BOOK_NOT_FOUND, error.getError());
    }

    @Test
    void movingAChapterForwardPushesTheRangeBack() {
        Book book = newBook();
        List<Long> ids = seedChapters(book, 1, 2, 3, 4);

        chapterService.update(book.getId(), ids.get(3), moveTo(2));

        assertEquals(List.of("1:C1", "2:C4", "3:C2", "4:C3"), chapterOrder(book));
    }

    @Test
    void movingAChapterBackwardPullsTheRangeForward() {
        Book book = newBook();
        List<Long> ids = seedChapters(book, 1, 2, 3, 4);

        chapterService.update(book.getId(), ids.get(1), moveTo(4));

        assertEquals(List.of("1:C1", "2:C3", "3:C4", "4:C2"), chapterOrder(book));
    }

    @Test
    void movingBeyondTheEndStopsAtTheLastOrder() {
        Book book = newBook();
        List<Long> ids = seedChapters(book, 1, 2, 3);

        chapterService.update(book.getId(), ids.get(0), moveTo(99));

        assertEquals(List.of("1:C2", "2:C3", "3:C1"), chapterOrder(book));
    }

    @Test
    void movingAChapterToItsOwnOrderChangesNothing() {
        Book book = newBook();
        List<Long> ids = seedChapters(book, 1, 2, 3);

        chapterService.update(book.getId(), ids.get(1), moveTo(2));

        assertEquals(List.of("1:C1", "2:C2", "3:C3"), chapterOrder(book));
    }

    @Test
    void updatingWithoutAnOrderKeepsEveryChapterInPlace() {
        Book book = newBook();
        List<Long> ids = seedChapters(book, 1, 2);

        chapterService.update(book.getId(), ids.get(1), ChapterUpdateRequest.builder().title("Tên mới").build());

        assertEquals(List.of("1:C1", "2:Tên mới"), chapterOrder(book));
    }

    @Test
    void updatingAChapterThroughAnotherBookIsRejected() {
        Book book = newBook();
        Book other = newBook();
        Long chapterId = seedChapters(book, 1).get(0);

        BusinessException error = assertThrows(BusinessException.class,
                () -> chapterService.update(other.getId(), chapterId,
                        ChapterUpdateRequest.builder().title("Tên mới").build()));

        assertEquals(BusinessError.CHAPTER_NOT_FOUND, error.getError());
        assertEquals("C1", chapterRepository.findById(chapterId).orElseThrow().getTitle());
    }

    @Test
    void shortChapterCanStillGenerateAudio() {
        Book book = newBook();
        Long chapterId = seedChapters(book, 1).get(0);

        chapterService.generateAudio(ChapterGenerateAudioRequest.builder()
                .bookId(book.getId()).chapterId(chapterId).build());

        verify(messageQueue).send(any());
    }

    private Book newBook() {
        Book book = new Book();
        book.setTitle("Sách");
        book.setPrice(BigDecimal.TEN);
        book.setDescription("Mô tả");
        book.setCoverImage("images/a.png");
        book.setContentFile("pdfs/a.pdf");
        book.setStatus(BookStatus.PUBLISHED);
        return bookRepository.insert(book);
    }

    /** Titles each seeded chapter "C" + its order, so a later assertion shows which chapter moved where. */
    private List<Long> seedChapters(Book book, int... orders) {
        List<Chapter> chapters = java.util.Arrays.stream(orders).mapToObj(order -> Chapter.builder()
                .title("C" + order).chapterOrder(order).rawText("Nội dung").build()).toList();
        return chapterRepository.saveAll(chapters, book).stream().map(Chapter::getId).toList();
    }

    private List<String> chapterOrder(Book book) {
        return chapterRepository.findByBookId(book.getId()).stream()
                .map(chapter -> chapter.getChapterOrder() + ":" + chapter.getTitle()).toList();
    }

    private static ChapterCreateRequest createRequest(String title, Integer order) {
        return ChapterCreateRequest.builder().title(title).chapterOrder(order).build();
    }

    private static ChapterUpdateRequest moveTo(int order) {
        return ChapterUpdateRequest.builder().chapterOrder(order).build();
    }

    @Test
    void paymentFlowUnlocksTheBookThroughThePorts() {
        when(storageService.createSignUrl(anyString(), anyLong())).thenReturn("https://signed");
        Long customerId = customerRepository.save(Customer.builder()
                .email("buyer@example.com").password("x").fullName("Người mua").build()).getId();
        Book book = new Book();
        book.setTitle("Sách");
        book.setPrice(BigDecimal.TEN);
        book.setDescription("Mô tả");
        book.setCoverImage("images/a.png");
        book.setContentFile("pdfs/a.pdf");
        book.setStatus(BookStatus.PUBLISHED);
        Long bookId = bookRepository.insert(book).getId();

        PaymentResponse created = paymentService.createPayment(customerId, bookId);

        assertEquals(PaymentStatus.PENDING, created.getStatus());
        assertEquals("Sách", created.getBookTitle());
        assertEquals("https://signed", created.getBookCover());
        assertTrue(created.getQrUrl().contains(created.getPaymentCode()));
        assertEquals(created.getId(), paymentService.createPayment(customerId, bookId).getId());
        assertEquals(created.getId(), paymentService.getPendingPaymentByBook(customerId, bookId).getId());
        assertFalse(customerBookService.hasPurchasedBook(customerId, bookId));

        paymentService.confirmPayment(created.getPaymentCode(), BigDecimal.TEN);
        paymentService.confirmPayment(created.getPaymentCode(), BigDecimal.TEN);

        assertTrue(customerBookService.hasPurchasedBook(customerId, bookId));
        assertEquals(List.of(bookId),
                customerBookService.getPurchasedBooks(customerId).stream().map(BookResponse::getId).toList());
        assertEquals(PaymentStatus.PAID, paymentService.getPaymentStatus(created.getId(), customerId).getStatus());
        assertNull(paymentService.getPendingPaymentByBook(customerId, bookId));
        assertFalse(customerBookService.hasPurchasedBook(null, bookId));
        BusinessException owned = assertThrows(BusinessException.class,
                () -> paymentService.createPayment(customerId, bookId));
        assertEquals(BusinessError.BOOK_ALREADY_OWNED, owned.getError());
    }

    @Test
    void underpaidPaymentDoesNotUnlockTheBook() {
        when(storageService.createSignUrl(anyString(), anyLong())).thenReturn("https://signed");
        Long customerId = customerRepository.save(Customer.builder()
                .email("underpaid@example.com").password("x").fullName("Người mua").build()).getId();
        Book book = new Book();
        book.setTitle("Sách");
        book.setPrice(BigDecimal.TEN);
        book.setDescription("Mô tả");
        book.setCoverImage("images/a.png");
        book.setContentFile("pdfs/a.pdf");
        book.setStatus(BookStatus.PUBLISHED);
        Long bookId = bookRepository.insert(book).getId();
        PaymentResponse created = paymentService.createPayment(customerId, bookId);

        BusinessException underpaid = assertThrows(BusinessException.class,
                () -> paymentService.confirmPayment(created.getPaymentCode(), BigDecimal.ONE));

        assertEquals(BusinessError.VALIDATION_FAILED, underpaid.getError());
        assertFalse(customerBookService.hasPurchasedBook(customerId, bookId));
        assertEquals(PaymentStatus.PENDING, paymentService.getPaymentStatus(created.getId(), customerId).getStatus());
    }

    @Test
    void confirmingAnUnknownPaymentCodeIsRejected() {
        BusinessException unknownCode = assertThrows(BusinessException.class,
                () -> paymentService.confirmPayment("SP0", BigDecimal.TEN));

        assertEquals(BusinessError.PAYMENT_NOT_FOUND, unknownCode.getError());
    }

    @Test
    void toggleFavoriteAddsThenRemovesTheBook() {
        when(storageService.createSignUrl(anyString(), anyLong())).thenReturn("https://signed");
        Long customerId = customerRepository.save(Customer.builder()
                .email("fan@example.com").password("x").fullName("Người thích").build()).getId();
        Book book = new Book();
        book.setTitle("Sách");
        book.setPrice(BigDecimal.TEN);
        book.setDescription("Mô tả");
        book.setCoverImage("images/a.png");
        book.setContentFile("pdfs/a.pdf");
        book.setStatus(BookStatus.PUBLISHED);
        Long bookId = bookRepository.insert(book).getId();

        BookFavoriteResponse added = bookFavoriteService.toggleFavorite(customerId, bookId);

        assertTrue(added.getIsFavorite());
        assertTrue(bookFavoriteService.isFavorite(customerId, bookId));
        assertEquals(List.of(bookId), bookFavoriteService.list(customerId).stream().map(BookResponse::getId).toList());

        BookFavoriteResponse removed = bookFavoriteService.toggleFavorite(customerId, bookId);

        assertFalse(removed.getIsFavorite());
        assertFalse(bookFavoriteService.isFavorite(customerId, bookId));
        assertEquals(List.of(), bookFavoriteService.list(customerId));
    }

    @Test
    void responsesLeaveNullFieldsOutOfTheJson() {
        assertEquals("{\"id\":1}", jsonMapper.writeValueAsString(BookResponse.builder().id(1L).build()));
    }
}
