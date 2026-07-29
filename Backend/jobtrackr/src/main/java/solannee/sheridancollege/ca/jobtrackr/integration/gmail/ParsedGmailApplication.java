package solannee.sheridancollege.ca.jobtrackr.integration.gmail;

import solannee.sheridancollege.ca.jobtrackr.model.ApplicationStatus;
import solannee.sheridancollege.ca.jobtrackr.model.DetectionConfidence;
import solannee.sheridancollege.ca.jobtrackr.model.EmploymentType;
import solannee.sheridancollege.ca.jobtrackr.model.GmailImportProvider;

import java.time.LocalDate;

public record ParsedGmailApplication(
        GmailImportProvider provider,
        DetectionConfidence confidence,
        String company,
        String jobTitle,
        String location,
        String jobUrl,
        LocalDate applicationDate,
        ApplicationStatus status,
        EmploymentType employmentType
) {
}
