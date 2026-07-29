package solannee.sheridancollege.ca.jobtrackr.dto.integration;

import solannee.sheridancollege.ca.jobtrackr.model.ApplicationStatus;
import solannee.sheridancollege.ca.jobtrackr.model.DetectionConfidence;
import solannee.sheridancollege.ca.jobtrackr.model.EmploymentType;
import solannee.sheridancollege.ca.jobtrackr.model.GmailImportProvider;
import solannee.sheridancollege.ca.jobtrackr.model.GmailImportState;

import java.time.Instant;
import java.time.LocalDate;

public record GmailImportCandidateResponse(
        Long id,
        GmailImportProvider provider,
        DetectionConfidence confidence,
        String company,
        String jobTitle,
        String location,
        String jobUrl,
        LocalDate applicationDate,
        ApplicationStatus status,
        EmploymentType employmentType,
        String sourceSubject,
        String sourceSender,
        Instant receivedAt,
        GmailImportState state,
        Long importedApplicationId,
        Instant detectedAt
) {
}
