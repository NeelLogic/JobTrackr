package solannee.sheridancollege.ca.jobtrackr.dto.insights;

import solannee.sheridancollege.ca.jobtrackr.model.ApplicationStatus;
import solannee.sheridancollege.ca.jobtrackr.model.EmploymentType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record AnalyticsResponse(
        AnalyticsRange range,
        LocalDate fromDate,
        LocalDate toDate,
        long applicationsInRange,
        long previousPeriodApplications,
        double applicationGrowthRate,
        double responseRate,
        double interviewRate,
        double offerRate,
        List<FunnelStageResponse> funnel,
        List<TrendPointResponse> trend,
        Map<ApplicationStatus, Long> applicationsByStatus,
        Map<EmploymentType, Long> applicationsByEmploymentType,
        List<CompanyInsightResponse> topCompanies
) {
}
