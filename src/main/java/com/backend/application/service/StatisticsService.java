package com.backend.application.service;

import com.backend.application.dto.response.statistics.PurchaseTrendResponse;
import com.backend.application.dto.response.statistics.StatisticsResponse;
import com.backend.domain.valueobject.BestsellerPeriod;

/** Dashboard figures that belong to no single aggregate; bestsellers are in BookService and CategoryService. */
public interface StatisticsService {

    /** @param months how many recent months the chart covers, clamped to 1..24 */
    StatisticsResponse getStatistics(int months);

    /** All purchases in the period, against the period right before it. */
    PurchaseTrendResponse getPurchaseTrend(BestsellerPeriod period);
}
