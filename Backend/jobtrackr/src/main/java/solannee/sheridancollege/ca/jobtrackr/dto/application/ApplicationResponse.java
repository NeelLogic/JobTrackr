package solannee.sheridancollege.ca.jobtrackr.dto.application;
import solannee.sheridancollege.ca.jobtrackr.model.*;import java.math.BigDecimal;import java.time.*;
public record ApplicationResponse(Long id,String company,String jobTitle,String location,String jobUrl,LocalDate applicationDate,ApplicationStatus status,
 EmploymentType employmentType,BigDecimal salaryMin,BigDecimal salaryMax,String salaryCurrency,String notes,LocalDate followUpDate,Instant createdAt,Instant updatedAt){}
