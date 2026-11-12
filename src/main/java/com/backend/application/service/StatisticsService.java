package com.backend.application.service;

import com.backend.application.dto.response.statistics.PurchaseTrendResponse;
import com.backend.application.dto.response.statistics.StatisticsResponse;
import com.backend.domain.valueobject.BestsellerPeriod;

/** Dashboard figures that belong to no single aggregate; bestsellers are in BookService and CategoryService. */
public interface StatisticsService {

    StatisticsResponse getStatistics(int months);

    PurchaseTrendResponse getPurchaseTrend(BestsellerPeriod period);
}
