package com.backend.infrastructure.persistence.projection;

/** One count split into a window and the window before it. */
public interface PeriodCountProjection {
    Long getPurchases();
    Long getPreviousPurchases();
}
