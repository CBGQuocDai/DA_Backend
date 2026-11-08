package com.backend.application.dto.response.statistics;

import lombok.Builder;
import lombok.Getter;

/** All purchases in a period (paid or free), against the period right before it. */
@Getter
@Builder
public class PurchaseTrendResponse {
    private long purchases;
    /** Absent for ALL, which has no earlier period. */
    private Long previousPurchases;
}
