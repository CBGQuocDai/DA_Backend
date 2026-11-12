package com.backend.application.service.impl;

import com.backend.application.adapter.repository.BookRepository;
import com.backend.application.adapter.repository.CustomerBookRepository;
import com.backend.application.adapter.repository.CustomerRepository;
import com.backend.application.adapter.repository.PaymentRepository;
import com.backend.application.adapter.repository.ReviewRepository;
import com.backend.application.dto.response.review.ReviewSummaryResponse;
import com.backend.application.dto.response.statistics.PurchaseTrendResponse;
import com.backend.application.dto.response.statistics.StatisticsResponse.MonthPoint;
import com.backend.application.dto.response.statistics.StatisticsResponse.Overview;
import com.backend.application.dto.response.statistics.StatisticsResponse;
import com.backend.application.service.StatisticsService;
import com.backend.domain.valueobject.BestsellerPeriod;
import com.backend.domain.valueobject.BookStatus;
import com.backend.domain.valueobject.MonthlyTotal;
import com.backend.domain.valueobject.PeriodCount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Tổng hợp số liệu dashboard từ các repository và sắp xếp theo tháng. */
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

    @Override
    public StatisticsResponse getStatistics(int months) {
        return build(Math.clamp(months, 1, MAX_MONTHS), YearMonth.now(ZONE));
    }

    @Override
    public PurchaseTrendResponse getPurchaseTrend(BestsellerPeriod period) {
        return purchaseTrend(period, Instant.now());
    }

    StatisticsResponse build(int months, YearMonth current) {
        // Reach back to last month even for a 1-month chart, so this month always has something to compare with
        YearMonth first = current.minusMonths(Math.max(months, 2) - 1L);
        Instant from = first.atDay(1).atStartOfDay(ZONE).toInstant();
        // One grouped query gives both: each purchase is one paid payment
        Map<YearMonth, MonthlyTotal> paid = byMonth(paymentRepository.sumPaidByMonth(from, ZONE));
        Function<YearMonth, MonthlyTotal> paidIn = month -> paid.getOrDefault(month, new MonthlyTotal(month, 0, BigDecimal.ZERO));

        List<MonthPoint> points = new ArrayList<>(months);
        for (YearMonth month = current.minusMonths(months - 1L); !month.isAfter(current); month = month.plusMonths(1)) {
            points.add(MonthPoint.builder()
                    .month(month.toString())
                    .revenue(paidIn.apply(month).amount())
                    .purchases(paidIn.apply(month).count())
                    .build());
        }

        YearMonth previous = current.minusMonths(1);
        ReviewSummaryResponse reviews = reviewRepository.getOverallSummary();
        Overview overview = Overview.builder()
                .revenueThisMonth(paidIn.apply(current).amount())
                .revenueLastMonth(paidIn.apply(previous).amount())
                .purchasesThisMonth(paidIn.apply(current).count())
                .purchasesLastMonth(paidIn.apply(previous).count())
                .customers(customerRepository.count())
                .buyers(customerBookRepository.countBuyers())
                .reviews(reviews.getTotalReviews())
                .averageRating(reviews.getAverageRating())
                .build();

        Map<BookStatus, Long> bookStatus = new EnumMap<>(BookStatus.class);
        for (BookStatus status : BookStatus.values()) {
            long total = bookRepository.findAllWithStats(null, null, null, status, 0, 1).getTotal();
            bookStatus.put(status, total);
        }

        return StatisticsResponse.builder().overview(overview).bookStatus(bookStatus).months(points).build();
    }

    PurchaseTrendResponse purchaseTrend(BestsellerPeriod period, Instant now) {
        PeriodCount count = customerBookRepository.countPurchases(period.endingAt(now));
        return PurchaseTrendResponse.builder().purchases(count.current()).previousPurchases(count.previous()).build();
    }

    private static Map<YearMonth, MonthlyTotal> byMonth(List<MonthlyTotal> totals) {
        return totals.stream().collect(Collectors.toMap(MonthlyTotal::month, Function.identity()));
    }
}
