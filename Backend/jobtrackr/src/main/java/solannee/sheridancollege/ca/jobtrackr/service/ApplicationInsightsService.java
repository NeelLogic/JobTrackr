package solannee.sheridancollege.ca.jobtrackr.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solannee.sheridancollege.ca.jobtrackr.dto.application.ApplicationResponse;
import solannee.sheridancollege.ca.jobtrackr.dto.dashboard.DashboardResponse;
import solannee.sheridancollege.ca.jobtrackr.dto.insights.AnalyticsRange;
import solannee.sheridancollege.ca.jobtrackr.dto.insights.AnalyticsResponse;
import solannee.sheridancollege.ca.jobtrackr.dto.insights.CompaniesResponse;
import solannee.sheridancollege.ca.jobtrackr.dto.insights.CompanyInsightResponse;
import solannee.sheridancollege.ca.jobtrackr.dto.insights.FollowUpResponse;
import solannee.sheridancollege.ca.jobtrackr.dto.insights.FunnelStageResponse;
import solannee.sheridancollege.ca.jobtrackr.dto.insights.TrendPointResponse;
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
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationInsightsService {

    private static final Set<ApplicationStatus> ACTIVE_STATUSES = EnumSet.of(
            ApplicationStatus.APPLIED,
            ApplicationStatus.ASSESSMENT,
            ApplicationStatus.INTERVIEW,
            ApplicationStatus.OFFER
    );
    private static final Set<ApplicationStatus> RESPONSE_STATUSES = EnumSet.of(
            ApplicationStatus.ASSESSMENT,
            ApplicationStatus.INTERVIEW,
            ApplicationStatus.OFFER,
            ApplicationStatus.REJECTED
    );
    private static final Set<String> COMPANY_SORT_FIELDS = Set.of(
            "applications", "company", "interviews", "offers", "recent"
    );

    private final JobApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository statusHistoryRepository;
    private final ApplicationMapper applicationMapper;

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(User user) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        List<JobApplication> applications = applicationRepository.findAllByUserId(user.getId());
        Map<Long, Set<ApplicationStatus>> reached = reachedStatuses(user.getId(), applications);
        Map<ApplicationStatus, Long> statusCounts = statusCounts(applications);
        long applied = countReached(applications, reached, ApplicationStatus.APPLIED);

        List<ApplicationResponse> recent = applications.stream()
                .sorted(Comparator.comparing(JobApplication::getUpdatedAt).reversed())
                .limit(5)
                .map(applicationMapper::toResponse)
                .toList();

        return new DashboardResponse(
                applications.size(),
                applications.stream()
                        .filter(application -> {
                            LocalDate date = activityDate(application);
                            return !date.isBefore(monthStart) && !date.isAfter(today);
                        })
                        .count(),
                statusCounts.get(ApplicationStatus.INTERVIEW),
                statusCounts.get(ApplicationStatus.OFFER),
                statusCounts.get(ApplicationStatus.REJECTED),
                applications.stream().filter(this::isActive).count(),
                applications.stream().filter(application -> isOverdue(application, today)).count(),
                applications.stream().filter(application -> isUpcoming(application, today)).count(),
                applications.stream().filter(this::isStale).count(),
                rate(countResponded(applications, reached), applied),
                rate(countReached(applications, reached, ApplicationStatus.INTERVIEW), applied),
                rate(countReached(applications, reached, ApplicationStatus.OFFER), applied),
                Map.copyOf(statusCounts),
                recent
        );
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse analytics(User user, AnalyticsRange range) {
        LocalDate today = LocalDate.now();
        List<JobApplication> allApplications = applicationRepository.findAllByUserId(user.getId());
        Map<Long, Set<ApplicationStatus>> reached = reachedStatuses(user.getId(), allApplications);
        LocalDate requestedStart = range.startDate(today);
        List<JobApplication> applications = allApplications.stream()
                .filter(application -> isWithin(activityDate(application), requestedStart, today))
                .toList();
        LocalDate effectiveStart = requestedStart != null
                ? requestedStart
                : applications.stream().map(this::activityDate).min(LocalDate::compareTo).orElse(null);

        long previousApplications = previousPeriodCount(allApplications, range, requestedStart);
        long applied = countReached(applications, reached, ApplicationStatus.APPLIED);
        long assessments = countReached(applications, reached, ApplicationStatus.ASSESSMENT);
        long interviews = countReached(applications, reached, ApplicationStatus.INTERVIEW);
        long offers = countReached(applications, reached, ApplicationStatus.OFFER);

        List<FunnelStageResponse> funnel = List.of(
                new FunnelStageResponse(ApplicationStatus.APPLIED, applied, rate(applied, applied)),
                new FunnelStageResponse(ApplicationStatus.ASSESSMENT, assessments, rate(assessments, applied)),
                new FunnelStageResponse(ApplicationStatus.INTERVIEW, interviews, rate(interviews, applied)),
                new FunnelStageResponse(ApplicationStatus.OFFER, offers, rate(offers, applied))
        );

        return new AnalyticsResponse(
                range,
                effectiveStart,
                today,
                applications.size(),
                previousApplications,
                growthRate(applications.size(), previousApplications, range),
                rate(countResponded(applications, reached), applied),
                rate(interviews, applied),
                rate(offers, applied),
                funnel,
                trend(applications, effectiveStart, today),
                Map.copyOf(statusCounts(applications)),
                Map.copyOf(employmentTypeCounts(applications)),
                companyInsights(applications, reached).stream().limit(5).toList()
        );
    }

    @Transactional(readOnly = true)
    public CompaniesResponse companies(User user, String search, String sort, String direction) {
        if (!COMPANY_SORT_FIELDS.contains(sort)) {
            throw new InvalidRequestException("Unsupported company sort field: " + sort);
        }
        boolean ascending;
        if ("asc".equalsIgnoreCase(direction)) {
            ascending = true;
        } else if ("desc".equalsIgnoreCase(direction)) {
            ascending = false;
        } else {
            throw new InvalidRequestException("Direction must be 'asc' or 'desc'");
        }

        List<JobApplication> applications = applicationRepository.findAllByUserId(user.getId());
        Map<Long, Set<ApplicationStatus>> reached = reachedStatuses(user.getId(), applications);
        Comparator<CompanyInsightResponse> comparator = companyComparator(sort);
        if (!ascending) {
            comparator = comparator.reversed();
        }

        String normalizedSearch = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        List<CompanyInsightResponse> companies = companyInsights(applications, reached).stream()
                .filter(company -> normalizedSearch.isEmpty()
                        || company.company().toLowerCase(Locale.ROOT).contains(normalizedSearch))
                .sorted(comparator.thenComparing(
                        CompanyInsightResponse::company,
                        String.CASE_INSENSITIVE_ORDER
                ))
                .toList();
        return new CompaniesResponse(companies.size(), companies);
    }

    @Transactional(readOnly = true)
    public FollowUpResponse followUps(User user) {
        LocalDate today = LocalDate.now();
        List<JobApplication> applications = applicationRepository.findAllByUserId(user.getId());
        List<ApplicationResponse> overdue = mapSorted(
                applications.stream().filter(application -> isOverdue(application, today)).toList(),
                Comparator.comparing(JobApplication::getFollowUpDate)
        );
        List<ApplicationResponse> dueToday = mapSorted(
                applications.stream()
                        .filter(this::isActive)
                        .filter(application -> today.equals(application.getFollowUpDate()))
                        .toList(),
                Comparator.comparing(JobApplication::getUpdatedAt).reversed()
        );
        List<ApplicationResponse> upcoming = mapSorted(
                applications.stream().filter(application -> isUpcoming(application, today)).toList(),
                Comparator.comparing(JobApplication::getFollowUpDate)
        );
        List<ApplicationResponse> stale = mapSorted(
                applications.stream().filter(this::isStale).toList(),
                Comparator.comparing(JobApplication::getUpdatedAt)
        );
        return new FollowUpResponse(
                overdue.size(),
                dueToday.size(),
                upcoming.size(),
                stale.size(),
                overdue,
                dueToday,
                upcoming,
                stale
        );
    }

    private Map<Long, Set<ApplicationStatus>> reachedStatuses(
            Long userId,
            List<JobApplication> applications
    ) {
        Map<Long, Set<ApplicationStatus>> reached = new HashMap<>();
        for (JobApplication application : applications) {
            reached.computeIfAbsent(application.getId(), ignored -> EnumSet.noneOf(ApplicationStatus.class))
                    .add(application.getStatus());
        }
        for (ApplicationStatusHistory history :
                statusHistoryRepository.findAllByUserIdOrderByChangedAtAsc(userId)) {
            reached.computeIfAbsent(
                    history.getApplication().getId(),
                    ignored -> EnumSet.noneOf(ApplicationStatus.class)
            ).add(history.getToStatus());
        }
        return reached;
    }

    private long countReached(
            List<JobApplication> applications,
            Map<Long, Set<ApplicationStatus>> reached,
            ApplicationStatus stage
    ) {
        return applications.stream()
                .filter(application -> hasReached(reached.get(application.getId()), stage))
                .count();
    }

    private long countResponded(
            List<JobApplication> applications,
            Map<Long, Set<ApplicationStatus>> reached
    ) {
        return applications.stream()
                .filter(application -> {
                    Set<ApplicationStatus> statuses = reached.get(application.getId());
                    return statuses != null && statuses.stream().anyMatch(RESPONSE_STATUSES::contains);
                })
                .count();
    }

    private boolean hasReached(Set<ApplicationStatus> statuses, ApplicationStatus stage) {
        if (statuses == null) {
            return false;
        }
        return switch (stage) {
            case APPLIED -> statuses.stream().anyMatch(status -> status != ApplicationStatus.SAVED);
            case ASSESSMENT -> statuses.stream().anyMatch(status ->
                    status == ApplicationStatus.ASSESSMENT
                            || status == ApplicationStatus.INTERVIEW
                            || status == ApplicationStatus.OFFER);
            case INTERVIEW -> statuses.contains(ApplicationStatus.INTERVIEW)
                    || statuses.contains(ApplicationStatus.OFFER);
            case OFFER -> statuses.contains(ApplicationStatus.OFFER);
            default -> statuses.contains(stage);
        };
    }

    private Map<ApplicationStatus, Long> statusCounts(List<JobApplication> applications) {
        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            counts.put(status, 0L);
        }
        applications.forEach(application ->
                counts.compute(application.getStatus(), (status, count) -> count + 1));
        return counts;
    }

    private Map<EmploymentType, Long> employmentTypeCounts(List<JobApplication> applications) {
        Map<EmploymentType, Long> counts = new EnumMap<>(EmploymentType.class);
        for (EmploymentType type : EmploymentType.values()) {
            counts.put(type, 0L);
        }
        applications.forEach(application ->
                counts.compute(application.getEmploymentType(), (type, count) -> count + 1));
        return counts;
    }

    private List<CompanyInsightResponse> companyInsights(
            List<JobApplication> applications,
            Map<Long, Set<ApplicationStatus>> reached
    ) {
        Map<String, List<JobApplication>> grouped = applications.stream()
                .collect(Collectors.groupingBy(
                        application -> application.getCompany().toLowerCase(Locale.ROOT),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        return grouped.values().stream()
                .map(group -> toCompanyInsight(group, reached))
                .sorted(Comparator.comparingLong(CompanyInsightResponse::totalApplications)
                        .reversed()
                        .thenComparing(CompanyInsightResponse::company, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private CompanyInsightResponse toCompanyInsight(
            List<JobApplication> applications,
            Map<Long, Set<ApplicationStatus>> reached
    ) {
        JobApplication latest = applications.stream()
                .max(Comparator.comparing(JobApplication::getUpdatedAt))
                .orElseThrow();
        return new CompanyInsightResponse(
                applications.getFirst().getCompany(),
                applications.size(),
                applications.stream().filter(this::isActive).count(),
                countReached(applications, reached, ApplicationStatus.INTERVIEW),
                countReached(applications, reached, ApplicationStatus.OFFER),
                applications.stream()
                        .map(JobApplication::getApplicationDate)
                        .filter(date -> date != null)
                        .max(LocalDate::compareTo)
                        .orElse(null),
                latest.getUpdatedAt()
        );
    }

    private Comparator<CompanyInsightResponse> companyComparator(String sort) {
        return switch (sort) {
            case "company" -> Comparator.comparing(
                    CompanyInsightResponse::company,
                    String.CASE_INSENSITIVE_ORDER
            );
            case "interviews" -> Comparator.comparingLong(CompanyInsightResponse::interviewsReached);
            case "offers" -> Comparator.comparingLong(CompanyInsightResponse::offersReached);
            case "recent" -> Comparator.comparing(
                    CompanyInsightResponse::lastActivityAt,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            default -> Comparator.comparingLong(CompanyInsightResponse::totalApplications);
        };
    }

    private List<TrendPointResponse> trend(
            List<JobApplication> applications,
            LocalDate from,
            LocalDate to
    ) {
        if (from == null) {
            return List.of();
        }
        boolean monthly = from.plusDays(120).isBefore(to);
        Function<LocalDate, LocalDate> bucket = monthly
                ? date -> date.withDayOfMonth(1)
                : date -> date.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        Map<LocalDate, Long> counts = applications.stream()
                .collect(Collectors.groupingBy(
                        application -> bucket.apply(activityDate(application)),
                        Collectors.counting()
                ));
        LocalDate cursor = bucket.apply(from);
        LocalDate last = bucket.apply(to);
        List<TrendPointResponse> points = new ArrayList<>();
        while (!cursor.isAfter(last)) {
            points.add(new TrendPointResponse(cursor, counts.getOrDefault(cursor, 0L)));
            cursor = monthly ? cursor.plusMonths(1) : cursor.plusWeeks(1);
        }
        return points;
    }

    private long previousPeriodCount(
            List<JobApplication> applications,
            AnalyticsRange range,
            LocalDate currentStart
    ) {
        if (range == AnalyticsRange.ALL_TIME || currentStart == null) {
            return 0;
        }
        LocalDate previousStart = currentStart.minusDays(range.days());
        LocalDate previousEnd = currentStart.minusDays(1);
        return applications.stream()
                .map(this::activityDate)
                .filter(date -> isWithin(date, previousStart, previousEnd))
                .count();
    }

    private double growthRate(long current, long previous, AnalyticsRange range) {
        if (range == AnalyticsRange.ALL_TIME) {
            return 0;
        }
        if (previous == 0) {
            return current == 0 ? 0 : 100;
        }
        return round(((current - previous) * 100.0) / previous);
    }

    private List<ApplicationResponse> mapSorted(
            List<JobApplication> applications,
            Comparator<JobApplication> comparator
    ) {
        return applications.stream().sorted(comparator).map(applicationMapper::toResponse).toList();
    }

    private boolean isActive(JobApplication application) {
        return ACTIVE_STATUSES.contains(application.getStatus());
    }

    private boolean isOverdue(JobApplication application, LocalDate today) {
        return isActive(application)
                && application.getFollowUpDate() != null
                && application.getFollowUpDate().isBefore(today);
    }

    private boolean isUpcoming(JobApplication application, LocalDate today) {
        return isActive(application)
                && application.getFollowUpDate() != null
                && application.getFollowUpDate().isAfter(today)
                && !application.getFollowUpDate().isAfter(today.plusDays(14));
    }

    private boolean isStale(JobApplication application) {
        return isActive(application)
                && application.getUpdatedAt() != null
                && application.getUpdatedAt().isBefore(Instant.now().minusSeconds(14L * 24 * 60 * 60));
    }

    private LocalDate activityDate(JobApplication application) {
        if (application.getApplicationDate() != null) {
            return application.getApplicationDate();
        }
        return application.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
    }

    private boolean isWithin(LocalDate date, LocalDate from, LocalDate to) {
        return (from == null || !date.isBefore(from)) && !date.isAfter(to);
    }

    private double rate(long numerator, long denominator) {
        return denominator == 0 ? 0 : round(numerator * 100.0 / denominator);
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
