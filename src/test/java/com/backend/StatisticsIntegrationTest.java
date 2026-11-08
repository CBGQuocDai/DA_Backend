package com.backend;

import com.backend.application.adapter.messaging.MessageQueue;
import com.backend.application.adapter.ocr.OcrService;
import com.backend.application.adapter.repository.CustomerRepository;
import com.backend.application.adapter.storage.StorageService;
import com.backend.application.dto.request.statistics.BestsellerPeriod;
import com.backend.application.dto.request.user.RegisterRequest;
import com.backend.application.dto.response.book.BookResponse;
import com.backend.application.dto.response.category.CategoryResponse;
import com.backend.application.dto.response.statistics.PurchaseTrendResponse;
import com.backend.application.dto.response.statistics.StatisticsResponse;
import com.backend.application.service.BookService;
import com.backend.application.service.CategoryService;
import com.backend.application.service.CustomerService;
import com.backend.application.service.FileManagementService;
import com.backend.application.service.StatisticsService;
import com.backend.domain.valueobject.BookStatus;
import com.backend.domain.valueobject.PaymentStatus;
import com.backend.infrastructure.persistence.entity.*;
import com.backend.infrastructure.persistence.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The dashboard queries against a real schema on H2. */
@SpringBootTest
@ActiveProfiles("test")
class StatisticsIntegrationTest {

    // Same overrides as the other integration tests, so they share one cached context
    @MockitoBean
    OcrService ocrService;
    @MockitoBean
    StorageService storageService;
    @MockitoBean
    FileManagementService fileManagementService;
    @MockitoBean
    MessageQueue messageQueue;

    @Autowired
    StatisticsService statisticsService;
    @Autowired
    BookService bookService;
    @Autowired
    CategoryService categoryService;
    @Autowired
    CustomerService customerService;
    @Autowired
    CustomerRepository customerRepository;
    @Autowired
    JpaCategoryRepository categoryRepository;
    @Autowired
    JpaBookRepository bookRepository;
    @Autowired
    JpaBookCategoryRepository bookCategoryRepository;
    @Autowired
    JpaCustomerBookRepository customerBookRepository;
    @Autowired
    JpaPaymentRepository paymentRepository;

    private Long newCustomer() {
        String email = "tk-" + UUID.randomUUID().toString().substring(0, 8) + "@da.vn";
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword("cu123456");
        req.setFullname("Người thử");
        customerService.register(req);
        return customerRepository.findByEmail(email).orElseThrow().getId();
    }

    private void own(Long customerId, Long bookId, Instant at) {
        JpaCustomerBookEntity owned = new JpaCustomerBookEntity();
        owned.setCustomerId(customerId);
        owned.setBookId(bookId);
        owned.setPurchaseAt(at);
        owned.setIsReadComplete(false);
        customerBookRepository.save(owned);
    }

    private void pay(Long customerId, Long bookId, PaymentStatus status, Instant at) {
        paymentRepository.save(JpaPaymentEntity.builder()
                .paymentCode("TK" + UUID.randomUUID().toString().substring(0, 12)).customerId(customerId)
                .bookId(bookId).amount(new BigDecimal("25000")).status(status)
                .paidAt(status == PaymentStatus.PAID ? at : null).createdAt(at).build());
    }

    @Test
    void dashboardGroupsPaymentsByVietnamMonth() {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        JpaBookEntity book = bookRepository.save(JpaBookEntity.builder()
                .title("Sách " + tag).price(new BigDecimal("25000")).status(BookStatus.PUBLISHED).build());
        ZoneId vietnam = ZoneId.of("Asia/Ho_Chi_Minh");
        YearMonth thisMonth = YearMonth.now(vietnam);
        // 00:30 on the 1st in Vietnam is still the previous day (and month) in UTC
        Instant justAfterMidnight = thisMonth.atDay(1).atStartOfDay(vietnam).plusMinutes(30).toInstant();
        StatisticsResponse before = statisticsService.getStatistics(6);

        Long customer = newCustomer();
        own(customer, book.getId(), justAfterMidnight);
        pay(customer, book.getId(), PaymentStatus.PAID, justAfterMidnight);
        pay(customer, book.getId(), PaymentStatus.PENDING, justAfterMidnight);

        StatisticsResponse after = statisticsService.getStatistics(6);

        assertEquals(6, after.getMonths().size());
        assertEquals(thisMonth.toString(), after.getMonths().getLast().getMonth());
        assertEquals(EnumSet.allOf(BookStatus.class), after.getBookStatus().keySet());
        // Only the paid payment counts, and it lands in this month rather than last
        assertEquals(0, before.getOverview().getRevenueThisMonth().add(new BigDecimal("25000"))
                .compareTo(after.getOverview().getRevenueThisMonth()));
        assertEquals(0, before.getOverview().getRevenueLastMonth().compareTo(after.getOverview().getRevenueLastMonth()));
        assertEquals(before.getMonths().getLast().getPurchases() + 1, after.getMonths().getLast().getPurchases());
        assertEquals(before.getOverview().getPurchasesThisMonth() + 1, after.getOverview().getPurchasesThisMonth());
        assertEquals(before.getOverview().getCustomers() + 1, after.getOverview().getCustomers());
        assertEquals(before.getOverview().getBuyers() + 1, after.getOverview().getBuyers());
    }

    @Test
    void bestsellersCompareTheWeekWithTheWeekBefore() {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        JpaCategoryEntity category = new JpaCategoryEntity();
        category.setName("Xu hướng " + tag);
        category = categoryRepository.save(category);
        JpaBookEntity book = bookRepository.save(JpaBookEntity.builder()
                .title("Đang lên " + tag).price(new BigDecimal("25000")).status(BookStatus.PUBLISHED).build());
        bookCategoryRepository.save(JpaBookCategoryEntity.builder().bookId(book.getId()).categoryId(category.getId()).build());

        Instant now = Instant.now();
        Instant lastWeek = now.minus(Duration.ofDays(10));
        Instant longAgo = now.minus(Duration.ofDays(60));
        // 5 this week, 1 the week before, 1 two months ago: a clear riser in the week view
        for (int i = 0; i < 5; i++) {
            Long customer = newCustomer();
            own(customer, book.getId(), now.minus(Duration.ofHours(i + 1)));
            pay(customer, book.getId(), i < 3 ? PaymentStatus.PAID : PaymentStatus.PENDING, now.minus(Duration.ofHours(i + 1)));
        }
        own(newCustomer(), book.getId(), lastWeek);
        own(newCustomer(), book.getId(), longAgo);

        BookResponse riser = bookService.getBestsellers(BestsellerPeriod.WEEK).stream()
                .filter(b -> b.getId().equals(book.getId())).findFirst().orElseThrow();
        assertEquals(5L, riser.getPurchases());
        assertEquals(1L, riser.getPreviousPurchases());
        assertEquals(0, new BigDecimal("75000").compareTo(riser.getRevenue()));
        assertEquals(0, new BigDecimal("25000").compareTo(riser.getPrice()));
        Long categoryId = category.getId();
        CategoryResponse trending = categoryService.getBestsellers(BestsellerPeriod.WEEK).stream()
                .filter(c -> c.getId().equals(categoryId)).findFirst().orElseThrow();
        assertEquals(5L, trending.getPurchases());
        assertEquals(1L, trending.getPreviousPurchases());

        PurchaseTrendResponse week = statisticsService.getPurchaseTrend(BestsellerPeriod.WEEK);
        assertTrue(week.getPurchases() >= 5);
        assertTrue(week.getPreviousPurchases() >= 1);

        CategoryResponse allTime = categoryService.getBestsellers(BestsellerPeriod.ALL).stream()
                .filter(c -> c.getId().equals(categoryId)).findFirst().orElseThrow();
        assertEquals(7L, allTime.getPurchases());
        assertNull(allTime.getPreviousPurchases());
        assertNull(statisticsService.getPurchaseTrend(BestsellerPeriod.ALL).getFrom());
    }
}
