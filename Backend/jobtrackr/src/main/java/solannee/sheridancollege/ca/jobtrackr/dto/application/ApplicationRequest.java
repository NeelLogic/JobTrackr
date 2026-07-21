package solannee.sheridancollege.ca.jobtrackr.dto.application;
import jakarta.validation.constraints.*;import solannee.sheridancollege.ca.jobtrackr.model.*;import java.math.BigDecimal;import java.time.LocalDate;
public record ApplicationRequest(@NotBlank @Size(max=120)String company,@NotBlank @Size(max=160)String jobTitle,@Size(max=160)String location,
 @Size(max=1000) @Pattern(regexp="^$|https?://.+",message="must be a valid HTTP(S) URL")String jobUrl,LocalDate applicationDate,@NotNull ApplicationStatus status,
 EmploymentType employmentType,@DecimalMin("0.00")BigDecimal salaryMin,@DecimalMin("0.00")BigDecimal salaryMax,@Pattern(regexp="^[A-Z]{3}$",message="must be a 3-letter currency code")String salaryCurrency,
 @Size(max=10000)String notes,LocalDate followUpDate){}
