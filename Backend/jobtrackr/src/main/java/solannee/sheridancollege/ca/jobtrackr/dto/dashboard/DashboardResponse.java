package solannee.sheridancollege.ca.jobtrackr.dto.dashboard;

import solannee.sheridancollege.ca.jobtrackr.dto.application.ApplicationResponse;
import solannee.sheridancollege.ca.jobtrackr.model.ApplicationStatus;

import java.util.List;
import java.util.Map;

public record DashboardResponse(
        long totalApplications,
        long applicationsThisMonth,
        long interviews,
        long offers,
        long rejections,
        Map<ApplicationStatus, Long> applicationsByStatus,
        List<ApplicationResponse> recentApplications
) {
}
