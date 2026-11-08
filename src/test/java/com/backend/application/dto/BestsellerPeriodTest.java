package com.backend.application.dto;

import com.backend.application.dto.request.statistics.BestsellerPeriod;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BestsellerPeriodTest {

    private static final Instant NOW = Instant.parse("2026-11-08T03:00:00Z");

    @Test
    void monthIsThirtyDaysAgainstTheThirtyBefore() {
        BestsellerPeriod.Window window = BestsellerPeriod.MONTH.endingAt(NOW);

        assertEquals(Instant.parse("2026-10-09T03:00:00Z"), window.from());
        assertEquals(NOW, window.to());
        assertEquals(Instant.parse("2026-09-09T03:00:00Z"), window.previousFrom());
    }

    @Test
    void allTimeStartsAtTheEpochWithNoPreviousWindow() {
        BestsellerPeriod.Window window = BestsellerPeriod.ALL.endingAt(NOW);

        assertEquals(Instant.EPOCH, window.from());
        assertNull(window.previousFrom());
    }
}
