package solannee.sheridancollege.ca.jobtrackr.dto.insights;

import solannee.sheridancollege.ca.jobtrackr.dto.application.ApplicationResponse;

import java.util.List;

public record FollowUpResponse(
        long overdueCount,
        long dueTodayCount,
        long upcomingCount,
        long staleCount,
        List<ApplicationResponse> overdue,
        List<ApplicationResponse> dueToday,
        List<ApplicationResponse> upcoming,
        List<ApplicationResponse> stale
) {
}
