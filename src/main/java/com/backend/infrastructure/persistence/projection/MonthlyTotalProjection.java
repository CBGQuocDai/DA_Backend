package com.backend.infrastructure.persistence.projection;

import java.math.BigDecimal;

/** One calendar month of a grouped count or sum (yr/mo: YEAR and MONTH are reserved words in H2). */
public interface MonthlyTotalProjection {
    Integer getYr();
    Integer getMo();
    Long getTotal();
    BigDecimal getAmount();
}
