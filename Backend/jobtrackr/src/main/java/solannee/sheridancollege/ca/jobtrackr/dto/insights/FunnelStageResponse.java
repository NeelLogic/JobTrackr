package solannee.sheridancollege.ca.jobtrackr.dto.insights;

import solannee.sheridancollege.ca.jobtrackr.model.ApplicationStatus;

public record FunnelStageResponse(
        ApplicationStatus stage,
        long applications,
        double conversionFromApplied
) {
}
