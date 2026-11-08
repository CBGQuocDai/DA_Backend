package com.backend.application.service.impl;

import com.backend.application.adapter.repository.BookRepository;
import com.backend.application.adapter.repository.CustomerBookRepository;
import com.backend.application.adapter.repository.CustomerRepository;
import com.backend.application.adapter.repository.PaymentRepository;
import com.backend.application.adapter.repository.ReviewRepository;
import com.backend.application.dto.response.review.ReviewSummaryResponse;
import com.backend.application.dto.response.statistics.PurchaseTrendResponse;
import com.backend.application.dto.response.statistics.StatisticsResponse;
import com.backend.domain.valueobject.BestsellerPeriod;
import com.backend.domain.valueobject.BookStatus;
import com.backend.domain.valueobject.MonthlyTotal;
import com.backend.domain.valueobject.PeriodCount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatisticsServiceImplTest {

    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final CustomerBookRepository customerBooks = mock(CustomerBookRepository.class);
    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final ReviewRepository reviews = mock(ReviewRepository.class);
    private final BookRepository books = mock(BookRepository.class);
    private final StatisticsServiceImpl service =
            new StatisticsServiceImpl(payments, customerBooks, customers, reviews, books);

    @BeforeEach
    void emptyStore() {
        when(payments.sumPaidByMonth(any(), any())).thenReturn(List.of());
        when(reviews.getOverallSummary()).thenReturn(
                ReviewSummaryResponse.builder().averageRating(4.3).totalReviews(7).distribution(Map.of()).build());
        when(books.countByStatus()).thenReturn(Map.of(BookStatus.PUBLISHED, 3L));
        when(customers.count()).thenReturn(5L);
        when(customerBooks.countBuyers()).thenReturn(2L);
    }

    @Test
    void linesMonthsUpAndFillsTheEmptyOnes() {
        when(payments.sumPaidByMonth(any(), any())).thenReturn(List.of(
                new MonthlyTotal(YearMonth.of(2026, 11), 4, new BigDecimal("847000")),
                new MonthlyTotal(YearMonth.of(2026, 10), 2, new BigDecimal("349000"))));

        StatisticsResponse stats = service.build(3, YearMonth.of(2026, 11));

        assertEquals(List.of("2026-09", "2026-10", "2026-11"),
                stats.getMonths().stream().map(StatisticsResponse.MonthPoint::getMonth).toList());
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getMonths().get(0).getRevenue()));
        assertEquals(4, stats.getMonths().get(2).getPurchases());
        assertEquals(0, new BigDecimal("847000").compareTo(stats.getOverview().getRevenueThisMonth()));
        assertEquals(0, new BigDecimal("349000").compareTo(stats.getOverview().getRevenueLastMonth()));
        assertEquals(4, stats.getOverview().getPurchasesThisMonth());
        assertEquals(2, stats.getOverview().getPurchasesLastMonth());
        assertEquals(5, stats.getOverview().getCustomers());
        assertEquals(2, stats.getOverview().getBuyers());
        assertEquals(4.3, stats.getOverview().getAverageRating());
        assertEquals(0L, stats.getBookStatus().get(BookStatus.FAILED));
    }

    @Test
    void oneMonthChartStillComparesWithLastMonth() {
        when(payments.sumPaidByMonth(any(), any())).thenReturn(List.of(
                new MonthlyTotal(YearMonth.of(2026, 10), 1, new BigDecimal("15000"))));

        StatisticsResponse stats = service.build(1, YearMonth.of(2026, 11));

        assertEquals(1, stats.getMonths().size());
        assertEquals(0, new BigDecimal("15000").compareTo(stats.getOverview().getRevenueLastMonth()));
    }

    @Test
    void purchaseTrendAsksForThePeriodWindow() {
        Instant now = Instant.parse("2026-11-08T03:00:00Z");
        when(customerBooks.countPurchases(BestsellerPeriod.WEEK.endingAt(now))).thenReturn(new PeriodCount(5, 2L));

        PurchaseTrendResponse trend = service.purchaseTrend(BestsellerPeriod.WEEK, now);

        assertEquals(5, trend.getPurchases());
        assertEquals(2L, trend.getPreviousPurchases());
    }
}
