package com.backend.domain.valueobject;

import java.math.BigDecimal;
import java.time.YearMonth;

/** How many of something happened in a calendar month, and what they added up to (zero when there is no amount). */
public record MonthlyTotal(YearMonth month, long count, BigDecimal amount) {
}
