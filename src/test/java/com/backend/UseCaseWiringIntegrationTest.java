package com.backend;

import com.backend.application.dto.request.book.chapter.GenerateAudioResult;
import com.backend.application.dto.request.voice.VoiceRequest;
import com.backend.application.dto.response.BookFavoriteResponse;
import com.backend.application.dto.response.book.BookResponse;
import com.backend.application.dto.response.payment.PaymentResponse;
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
import static org.mockito.Mockito.doThrow;
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

    @Autowired
    VoiceService voiceService;
    @Autowired
    VoiceRepository voiceRepository;
    @Autowired
    ChapterService chapterService;
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
