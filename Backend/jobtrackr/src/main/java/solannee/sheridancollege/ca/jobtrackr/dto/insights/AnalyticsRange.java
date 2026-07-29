package solannee.sheridancollege.ca.jobtrackr.dto.insights;

import solannee.sheridancollege.ca.jobtrackr.exception.InvalidRequestException;

import java.time.LocalDate;
import java.util.Locale;

public enum AnalyticsRange {
    THIRTY_DAYS(30),
    NINETY_DAYS(90),
    SIX_MONTHS(180),
    ALL_TIME(0);

    private final int days;

    AnalyticsRange(int days) {
        this.days = days;
    }

    public int days() {
        return days;
    }

    public LocalDate startDate(LocalDate today) {
        return this == ALL_TIME ? null : today.minusDays(days - 1L);
    }

    public static AnalyticsRange from(String value) {
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidRequestException(
                    "Range must be THIRTY_DAYS, NINETY_DAYS, SIX_MONTHS, or ALL_TIME"
            );
        }
    }
}
