package solannee.sheridancollege.ca.jobtrackr.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import solannee.sheridancollege.ca.jobtrackr.dto.application.ApplicationRequest;
import solannee.sheridancollege.ca.jobtrackr.exception.InvalidRequestException;
import solannee.sheridancollege.ca.jobtrackr.exception.ResourceNotFoundException;
import solannee.sheridancollege.ca.jobtrackr.mapper.ApplicationMapper;
import solannee.sheridancollege.ca.jobtrackr.model.*;
import solannee.sheridancollege.ca.jobtrackr.repository.JobApplicationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    @Mock JobApplicationRepository repository;
    private JobApplicationService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new JobApplicationService(repository, new ApplicationMapper());
        user = new User();
        user.setId(7L);
    }

    @Test
    void createNormalizesTextAndCurrencyAndAssignsOwner() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ApplicationRequest request = request(ApplicationStatus.APPLIED, LocalDate.now(),
                new BigDecimal("70000"), new BigDecimal("90000"), " cad ");

        service.create(user, request);

        ArgumentCaptor<JobApplication> saved = ArgumentCaptor.forClass(JobApplication.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(user);
        assertThat(saved.getValue().getCompany()).isEqualTo("Acme");
        assertThat(saved.getValue().getLocation()).isNull();
        assertThat(saved.getValue().getSalaryCurrency()).isEqualTo("CAD");
    }

    @Test
    void rejectsSalaryWithoutCurrency() {
        ApplicationRequest request = request(ApplicationStatus.SAVED, null,
                new BigDecimal("70000"), null, null);

        assertThatThrownBy(() -> service.create(user, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Salary currency is required when salary information is provided");
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsNonSavedApplicationWithoutApplicationDate() {
        ApplicationRequest request = request(ApplicationStatus.INTERVIEW, null, null, null, null);

        assertThatThrownBy(() -> service.create(user, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Application date is required unless the application is saved");
    }

    @Test
    void rejectsUnsupportedSortField() {
        assertThatThrownBy(() -> service.list(user, null, null, null, 0, 10, "user.passwordHash", "asc"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Unsupported sort field");
        verifyNoInteractions(repository);
    }

    @Test
    void missingOrUnownedApplicationIsReportedAsNotFound() {
        when(repository.findByIdAndUserId(99L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(user, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Application not found");
    }

    private ApplicationRequest request(
            ApplicationStatus status,
            LocalDate applicationDate,
            BigDecimal salaryMin,
            BigDecimal salaryMax,
            String currency
    ) {
        return new ApplicationRequest(
                " Acme ", " Engineer ", " ", "https://example.com/job",
                applicationDate, status, EmploymentType.FULL_TIME,
                salaryMin, salaryMax, currency, " notes ", null
        );
    }
}
