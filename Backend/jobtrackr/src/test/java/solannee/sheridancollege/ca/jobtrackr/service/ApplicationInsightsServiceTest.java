package solannee.sheridancollege.ca.jobtrackr.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import solannee.sheridancollege.ca.jobtrackr.dto.insights.AnalyticsRange;
import solannee.sheridancollege.ca.jobtrackr.dto.insights.AnalyticsResponse;
import solannee.sheridancollege.ca.jobtrackr.dto.insights.CompaniesResponse;
import solannee.sheridancollege.ca.jobtrackr.dto.insights.FollowUpResponse;
import solannee.sheridancollege.ca.jobtrackr.exception.InvalidRequestException;
import solannee.sheridancollege.ca.jobtrackr.mapper.ApplicationMapper;
import solannee.sheridancollege.ca.jobtrackr.model.ApplicationStatus;
import solannee.sheridancollege.ca.jobtrackr.model.ApplicationStatusHistory;
import solannee.sheridancollege.ca.jobtrackr.model.EmploymentType;
import solannee.sheridancollege.ca.jobtrackr.model.JobApplication;
import solannee.sheridancollege.ca.jobtrackr.model.User;
import solannee.sheridancollege.ca.jobtrackr.repository.ApplicationStatusHistoryRepository;
import solannee.sheridancollege.ca.jobtrackr.repository.JobApplicationRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationInsightsServiceTest {

    @Mock JobApplicationRepository applicationRepository;
    @Mock ApplicationStatusHistoryRepository historyRepository;

    private ApplicationInsightsService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new ApplicationInsightsService(
                applicationRepository,
                historyRepository,
                new ApplicationMapper()
        );
        user = new User();
        user.setId(9L);
    }

    @Test
    void analyticsUsesStatusHistoryForReachedStageConversions() {
        JobApplication rejected = application(
                1L,
                "Acme",
                ApplicationStatus.REJECTED,
                LocalDate.now().minusDays(5),
                Instant.now()
        );
        JobApplication offered = application(
                2L,
                "Beta",
                ApplicationStatus.OFFER,
                LocalDate.now().minusDays(12),
                Instant.now()
        );
        JobApplication older = application(
                3L,
                "Older Co",
                ApplicationStatus.APPLIED,
                LocalDate.now().minusDays(70),
                Instant.now()
        );
        when(applicationRepository.findAllByUserId(9L)).thenReturn(List.of(rejected, offered, older));
        when(historyRepository.findAllByUserIdOrderByChangedAtAsc(9L)).thenReturn(List.of(
                history(rejected, ApplicationStatus.APPLIED),
                history(rejected, ApplicationStatus.INTERVIEW),
                history(rejected, ApplicationStatus.REJECTED),
                history(offered, ApplicationStatus.OFFER)
        ));

        AnalyticsResponse response = service.analytics(user, AnalyticsRange.THIRTY_DAYS);

        assertThat(response.applicationsInRange()).isEqualTo(2);
        assertThat(response.responseRate()).isEqualTo(100);
        assertThat(response.interviewRate()).isEqualTo(100);
        assertThat(response.offerRate()).isEqualTo(50);
        assertThat(response.funnel())
                .extracting(stage -> stage.applications())
                .containsExactly(2L, 2L, 2L, 1L);
        assertThat(response.applicationsByStatus().get(ApplicationStatus.REJECTED)).isEqualTo(1);
        assertThat(response.topCompanies()).hasSize(2);
        verify(applicationRepository).findAllByUserId(9L);
        verify(historyRepository).findAllByUserIdOrderByChangedAtAsc(9L);
    }

    @Test
    void companiesGroupsNamesCaseInsensitivelyAndSupportsSearch() {
        JobApplication first = application(
                1L,
                "Acme",
                ApplicationStatus.INTERVIEW,
                LocalDate.now(),
                Instant.now().minus(2, ChronoUnit.DAYS)
        );
        JobApplication second = application(
                2L,
                "ACME",
                ApplicationStatus.OFFER,
                LocalDate.now(),
                Instant.now()
        );
        JobApplication other = application(
                3L,
                "Beta",
                ApplicationStatus.APPLIED,
                LocalDate.now(),
                Instant.now()
        );
        when(applicationRepository.findAllByUserId(9L)).thenReturn(List.of(first, second, other));
        when(historyRepository.findAllByUserIdOrderByChangedAtAsc(9L)).thenReturn(List.of());

        CompaniesResponse response = service.companies(user, "acm", "offers", "desc");

        assertThat(response.totalCompanies()).isEqualTo(1);
        assertThat(response.companies().getFirst().company()).isEqualTo("Acme");
        assertThat(response.companies().getFirst().totalApplications()).isEqualTo(2);
        assertThat(response.companies().getFirst().interviewsReached()).isEqualTo(2);
        assertThat(response.companies().getFirst().offersReached()).isEqualTo(1);
    }

    @Test
    void followUpsSeparateOverdueTodayUpcomingAndStaleApplications() {
        Instant recent = Instant.now();
        JobApplication overdue = application(
                1L, "Acme", ApplicationStatus.APPLIED, LocalDate.now(), recent
        );
        overdue.setFollowUpDate(LocalDate.now().minusDays(1));
        JobApplication today = application(
                2L, "Beta", ApplicationStatus.INTERVIEW, LocalDate.now(), recent
        );
        today.setFollowUpDate(LocalDate.now());
        JobApplication upcoming = application(
                3L, "Gamma", ApplicationStatus.ASSESSMENT, LocalDate.now(), recent
        );
        upcoming.setFollowUpDate(LocalDate.now().plusDays(7));
        JobApplication stale = application(
                4L,
                "Delta",
                ApplicationStatus.APPLIED,
                LocalDate.now().minusDays(30),
                Instant.now().minus(20, ChronoUnit.DAYS)
        );
        JobApplication closed = application(
                5L, "Closed", ApplicationStatus.REJECTED, LocalDate.now(), recent
        );
        closed.setFollowUpDate(LocalDate.now().minusDays(3));
        when(applicationRepository.findAllByUserId(9L))
                .thenReturn(List.of(overdue, today, upcoming, stale, closed));

        FollowUpResponse response = service.followUps(user);

        assertThat(response.overdue()).extracting(application -> application.company())
                .containsExactly("Acme");
        assertThat(response.dueToday()).extracting(application -> application.company())
                .containsExactly("Beta");
        assertThat(response.upcoming()).extracting(application -> application.company())
                .containsExactly("Gamma");
        assertThat(response.stale()).extracting(application -> application.company())
                .containsExactly("Delta");
    }

    @Test
    void rejectsUnsupportedCompanySort() {
        assertThatThrownBy(() -> service.companies(user, null, "password", "asc"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Unsupported company sort field");
    }

    private JobApplication application(
            Long id,
            String company,
            ApplicationStatus status,
            LocalDate applicationDate,
            Instant updatedAt
    ) {
        JobApplication application = new JobApplication();
        application.setId(id);
        application.setUser(user);
        application.setCompany(company);
        application.setJobTitle("Software Engineer");
        application.setStatus(status);
        application.setEmploymentType(EmploymentType.FULL_TIME);
        application.setApplicationDate(applicationDate);
        application.setCreatedAt(updatedAt);
        application.setUpdatedAt(updatedAt);
        return application;
    }

    private ApplicationStatusHistory history(
            JobApplication application,
            ApplicationStatus toStatus
    ) {
        ApplicationStatusHistory history = new ApplicationStatusHistory();
        history.setApplication(application);
        history.setUser(user);
        history.setToStatus(toStatus);
        history.setChangedAt(Instant.now());
        return history;
    }
}
