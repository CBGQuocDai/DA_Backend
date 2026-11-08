package com.backend.domain.valueobject;

/** A count in a period, and in the period before it (null when there is none to compare with). */
public record PeriodCount(long current, Long previous) {
}
