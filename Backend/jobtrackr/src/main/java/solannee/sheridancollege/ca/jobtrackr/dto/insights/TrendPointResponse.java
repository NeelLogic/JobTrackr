package solannee.sheridancollege.ca.jobtrackr.dto.insights;

import java.time.LocalDate;

public record TrendPointResponse(
        LocalDate periodStart,
        long applications
) {
}
