package com.backend.domain.valueobject;

import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.Instant;

/** Rolling windows ending now; each is compared with the window of the same length right before it. */
@RequiredArgsConstructor
public enum BestsellerPeriod {
    WEEK(7),
    MONTH(30),
    QUARTER(90),
    YEAR(365),
    /** Everything so far; there is no earlier window to compare with. */
    ALL(0);

    private final int days;

    /**
     * [from, to) is the period; [previousFrom, from) the one before it, or null for {@link #ALL}.
     * ALL starts at the epoch: nothing was bought before it, and it keeps queries bounded on both ends.
     */
    public record Window(Instant from, Instant to, Instant previousFrom) {

        public boolean hasPrevious() {
            return previousFrom != null;
        }

        /** Start of everything a query has to read: the previous window when there is one. */
        public Instant since() {
            return hasPrevious() ? previousFrom : from;
        }
    }

    public Window endingAt(Instant now) {
        if (days == 0) {
            return new Window(Instant.EPOCH, now, null);
        }
        Instant from = now.minus(Duration.ofDays(days));
        return new Window(from, now, from.minus(Duration.ofDays(days)));
    }
}
