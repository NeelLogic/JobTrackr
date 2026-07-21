package solannee.sheridancollege.ca.jobtrackr.dto.application;

import solannee.sheridancollege.ca.jobtrackr.model.ApplicationStatus;
import solannee.sheridancollege.ca.jobtrackr.model.EmploymentType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ApplicationResponse(
        Long id,
        String company,
        String jobTitle,
        String location,
        String jobUrl,
        LocalDate applicationDate,
        ApplicationStatus status,
        EmploymentType employmentType,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String salaryCurrency,
        String notes,
        LocalDate followUpDate,
        Instant createdAt,
        Instant updatedAt
) {
}
