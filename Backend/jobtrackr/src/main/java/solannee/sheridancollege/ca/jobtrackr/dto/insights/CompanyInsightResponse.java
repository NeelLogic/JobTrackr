package solannee.sheridancollege.ca.jobtrackr.dto.insights;

import java.time.Instant;
import java.time.LocalDate;

public record CompanyInsightResponse(
        String company,
        long totalApplications,
        long activeApplications,
        long interviewsReached,
        long offersReached,
        LocalDate latestApplicationDate,
        Instant lastActivityAt
) {
}
