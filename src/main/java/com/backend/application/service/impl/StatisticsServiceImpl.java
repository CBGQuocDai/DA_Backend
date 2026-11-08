package com.backend.application.service.impl;

import com.backend.application.adapter.repository.BookRepository;
import com.backend.application.adapter.repository.CustomerBookRepository;
import com.backend.application.adapter.repository.CustomerRepository;
import com.backend.application.adapter.repository.PaymentRepository;
import com.backend.application.adapter.repository.ReviewRepository;
import com.backend.application.dto.request.statistics.BestsellerPeriod;
import com.backend.application.dto.response.review.ReviewSummaryResponse;
import com.backend.application.dto.response.statistics.PurchaseTrendResponse;
import com.backend.application.dto.response.statistics.StatisticsResponse;
import com.backend.application.dto.response.statistics.StatisticsResponse.MonthPoint;
import com.backend.application.dto.response.statistics.StatisticsResponse.Overview;
import com.backend.application.service.StatisticsService;
import com.backend.domain.valueobject.BookStatus;
import com.backend.domain.valueobject.MonthlyTotal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Puts the dashboard together from each aggregate's own repository; every figure is a grouped query there,
 * this class only lines the months up and picks this month and last.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsServiceImpl implements StatisticsService {

    static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    static final int MAX_MONTHS = 24;

    private final PaymentRepository paymentRepository;
    private final CustomerBookRepository customerBookRepository;
    private final CustomerRepository customerRepository;
    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final Clock clock = Clock.system(ZONE);

    @Override
    public StatisticsResponse getStatistics(int months) {
        return build(Math.clamp(months, 1, MAX_MONTHS), YearMonth.now(clock));
    }

    @Override
    public PurchaseTrendResponse getPurchaseTrend(BestsellerPeriod period) {
        return purchaseTrend(period, clock.instant());
    }

    StatisticsResponse build(int months, YearMonth current) {
        // Reach back to last month even for a 1-month chart, so this month always has something to compare with
        YearMonth first = current.minusMonths(Math.max(months, 2) - 1L);
        Instant from = first.atDay(1).atStartOfDay(ZONE).toInstant();
        // One grouped query gives both: each purchase is one paid payment
        Map<YearMonth, MonthlyTotal> paid = byMonth(paymentRepository.sumPaidByMonth(from, ZONE));

        List<MonthPoint> points = new ArrayList<>(months);
        for (YearMonth month = current.minusMonths(months - 1L); !month.isAfter(current); month = month.plusMonths(1)) {
            points.add(MonthPoint.builder()
                    .month(month.toString())
                    .revenue(amount(paid, month))
                    .purchases(count(paid, month))
                    .build());
        }

        YearMonth previous = current.minusMonths(1);
        ReviewSummaryResponse reviews = reviewRepository.getOverallSummary();
        Overview overview = Overview.builder()
                .revenueThisMonth(amount(paid, current))
                .revenueLastMonth(amount(paid, previous))
                .purchasesThisMonth(count(paid, current))
                .purchasesLastMonth(count(paid, previous))
                .customers(customerRepository.count())
                .buyers(customerBookRepository.countBuyers())
                .reviews(reviews.getTotalReviews())
                .averageRating(reviews.getAverageRating())
                .build();

        Map<BookStatus, Long> counted = bookRepository.countByStatus();
        Map<BookStatus, Long> bookStatus = new EnumMap<>(BookStatus.class);
        for (BookStatus status : BookStatus.values()) {
            bookStatus.put(status, counted.getOrDefault(status, 0L));
        }

        return StatisticsResponse.builder().overview(overview).bookStatus(bookStatus).months(points).build();
    }

    PurchaseTrendResponse purchaseTrend(BestsellerPeriod period, Instant now) {
        BestsellerPeriod.Window window = period.endingAt(now);
        boolean comparable = window.previousFrom() != null;
        return PurchaseTrendResponse.builder()
                .period(period)
                .from(comparable ? window.from() : null)
                .to(window.to())
                .purchases(customerBookRepository.countPurchases(window.from(), window.to()))
                .previousPurchases(comparable
                        ? customerBookRepository.countPurchases(window.previousFrom(), window.from()) : null)
                .build();
    }

    private static Map<YearMonth, MonthlyTotal> byMonth(List<MonthlyTotal> totals) {
        return totals.stream().collect(Collectors.toMap(MonthlyTotal::month, Function.identity()));
    }

    private static long count(Map<YearMonth, MonthlyTotal> totals, YearMonth month) {
        MonthlyTotal total = totals.get(month);
        return total == null ? 0 : total.count();
    }

    private static BigDecimal amount(Map<YearMonth, MonthlyTotal> totals, YearMonth month) {
        MonthlyTotal total = totals.get(month);
        return total == null ? BigDecimal.ZERO : total.amount();
    }
}
