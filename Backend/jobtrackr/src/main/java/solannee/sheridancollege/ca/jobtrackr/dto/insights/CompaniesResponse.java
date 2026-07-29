package solannee.sheridancollege.ca.jobtrackr.dto.insights;

import java.util.List;

public record CompaniesResponse(
        int totalCompanies,
        List<CompanyInsightResponse> companies
) {
}
