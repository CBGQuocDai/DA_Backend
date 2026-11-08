package com.backend.application.dto.response.statistics;

import com.backend.application.dto.request.statistics.BestsellerPeriod;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/** All purchases in a period (paid or free), against the period right before it. */
@Getter
@Builder
public class PurchaseTrendResponse {
    private BestsellerPeriod period;
    /** Start of the period; absent for ALL. */
    private Instant from;
    private Instant to;
    private long purchases;
    /** Absent for ALL, which has no earlier period. */
    private Long previousPurchases;
}
