package solannee.sheridancollege.ca.jobtrackr.dto.application;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import solannee.sheridancollege.ca.jobtrackr.model.ApplicationStatus;
import solannee.sheridancollege.ca.jobtrackr.model.EmploymentType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ApplicationRequest(
        @NotBlank @Size(max = 120) String company,
        @NotBlank @Size(max = 160) String jobTitle,
        @Size(max = 160) String location,
        @Size(max = 1000)
        @Pattern(regexp = "^$|https?://\\S+$", message = "must be a valid HTTP(S) URL")
        String jobUrl,
        @PastOrPresent(message = "must not be in the future") LocalDate applicationDate,
        @NotNull ApplicationStatus status,
        @NotNull EmploymentType employmentType,
        @DecimalMin(value = "0.00", inclusive = true) BigDecimal salaryMin,
        @DecimalMin(value = "0.00", inclusive = true) BigDecimal salaryMax,
        @Pattern(regexp = "^$|[A-Za-z]{3}$", message = "must be a 3-letter currency code")
        String salaryCurrency,
        @Size(max = 10000) String notes,
        LocalDate followUpDate
) {
}
