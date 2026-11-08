package com.backend.application.dto.response.statistics;

import com.backend.domain.valueobject.BookStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Admin dashboard figures. Months and "this month" follow Vietnam time (Asia/Ho_Chi_Minh). */
@Getter
@Builder
public class StatisticsResponse {

    private Overview overview;
    /** Every status is present, zero when no book has it. */
    private Map<BookStatus, Long> bookStatus;
    /** Oldest first, one entry per month including empty ones. */
    private List<MonthPoint> months;

    @Getter
    @Builder
    public static class Overview {
        private BigDecimal revenueThisMonth;
        private BigDecimal revenueLastMonth;
        /** Paid purchases this month. */
        private long purchasesThisMonth;
        private long purchasesLastMonth;
        private long customers;
        /** Customers who own at least one book. */
        private long buyers;
        private long reviews;
        private double averageRating;
    }

    @Getter
    @Builder
    public static class MonthPoint {
        /** yyyy-MM */
        private String month;
        private BigDecimal revenue;
        /** Paid purchases in the month; every purchase is one paid payment. */
        private long purchases;
    }
}
