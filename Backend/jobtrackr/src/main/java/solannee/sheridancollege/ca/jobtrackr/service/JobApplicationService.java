package solannee.sheridancollege.ca.jobtrackr.service;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solannee.sheridancollege.ca.jobtrackr.dto.application.ApplicationRequest;
import solannee.sheridancollege.ca.jobtrackr.dto.application.ApplicationResponse;
import solannee.sheridancollege.ca.jobtrackr.dto.application.PageResponse;
import solannee.sheridancollege.ca.jobtrackr.dto.dashboard.DashboardResponse;
import solannee.sheridancollege.ca.jobtrackr.exception.InvalidRequestException;
import solannee.sheridancollege.ca.jobtrackr.exception.ResourceNotFoundException;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class JobApplicationService {

    private static final Set<String> SORT_FIELDS = Set.of(
            "company", "jobTitle", "applicationDate", "status", "createdAt", "updatedAt", "followUpDate"
    );

    private final JobApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository statusHistoryRepository;
    private final ApplicationMapper applicationMapper;

    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> list(
            User user,
            String search,
            ApplicationStatus status,
            EmploymentType employmentType,
            int page,
            int size,
            String sort,
            String direction
    ) {
        if (!SORT_FIELDS.contains(sort)) {
            throw new InvalidRequestException("Unsupported sort field: " + sort);
        }

        Sort.Direction sortDirection = Sort.Direction.fromOptionalString(direction)
                .orElseThrow(() -> new InvalidRequestException("Direction must be 'asc' or 'desc'"));
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Specification<JobApplication> specification = ownedBy(user.getId())
                .and(hasStatus(status))
                .and(hasEmploymentType(employmentType))
                .and(matchesSearch(search));

        Page<ApplicationResponse> result = applicationRepository.findAll(specification, pageRequest)
                .map(applicationMapper::toResponse);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse get(User user, Long id) {
        return applicationMapper.toResponse(findOwnedApplication(user.getId(), id));
    }

    @Transactional
    public ApplicationResponse create(User user, ApplicationRequest request) {
        JobApplication application = new JobApplication();
        application.setUser(user);
        copyRequest(request, application);
        JobApplication saved = applicationRepository.save(application);
        recordStatusChange(saved, null, saved.getStatus());
        return applicationMapper.toResponse(saved);
    }

    @Transactional
    public ApplicationResponse update(User user, Long id, ApplicationRequest request) {
        JobApplication application = findOwnedApplication(user.getId(), id);
        ApplicationStatus previousStatus = application.getStatus();
        copyRequest(request, application);
        JobApplication saved = applicationRepository.save(application);
        if (previousStatus != saved.getStatus()) {
            recordStatusChange(saved, previousStatus, saved.getStatus());
        }
        return applicationMapper.toResponse(saved);
    }

    @Transactional
    public void delete(User user, Long id) {
        applicationRepository.delete(findOwnedApplication(user.getId(), id));
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(User user) {
        Long userId = user.getId();
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            counts.put(status, applicationRepository.countByUserIdAndStatus(userId, status));
        }

        List<ApplicationResponse> recent = applicationRepository.findTop5ByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(applicationMapper::toResponse)
                .toList();

        return new DashboardResponse(
                applicationRepository.countByUserId(userId),
                applicationRepository.countByUserIdAndApplicationDateBetween(userId, monthStart, monthEnd),
                counts.get(ApplicationStatus.INTERVIEW),
                counts.get(ApplicationStatus.OFFER),
                counts.get(ApplicationStatus.REJECTED),
                Collections.unmodifiableMap(counts),
                recent
        );
    }

    private JobApplication findOwnedApplication(Long userId, Long applicationId) {
        return applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
    }

    private void recordStatusChange(
            JobApplication application,
            ApplicationStatus fromStatus,
            ApplicationStatus toStatus
    ) {
        ApplicationStatusHistory history = new ApplicationStatusHistory();
        history.setApplication(application);
        history.setUser(application.getUser());
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setChangedAt(Instant.now());
        statusHistoryRepository.save(history);
    }

    private void copyRequest(ApplicationRequest request, JobApplication application) {
        validateBusinessRules(request);
        application.setCompany(request.company().trim());
        application.setJobTitle(request.jobTitle().trim());
        application.setLocation(normalize(request.location()));
        application.setJobUrl(normalize(request.jobUrl()));
        application.setApplicationDate(request.applicationDate());
        application.setStatus(request.status());
        application.setEmploymentType(request.employmentType());
        application.setSalaryMin(request.salaryMin());
        application.setSalaryMax(request.salaryMax());
        application.setSalaryCurrency(normalizeCurrency(request.salaryCurrency()));
        application.setNotes(normalize(request.notes()));
        application.setFollowUpDate(request.followUpDate());
    }

    private void validateBusinessRules(ApplicationRequest request) {
        if (request.salaryMin() != null && request.salaryMax() != null
                && request.salaryMin().compareTo(request.salaryMax()) > 0) {
            throw new InvalidRequestException("Minimum salary cannot exceed maximum salary");
        }
        if ((request.salaryMin() != null || request.salaryMax() != null)
                && normalize(request.salaryCurrency()) == null) {
            throw new InvalidRequestException("Salary currency is required when salary information is provided");
        }
        if (request.status() != ApplicationStatus.SAVED && request.applicationDate() == null) {
            throw new InvalidRequestException("Application date is required unless the application is saved");
        }
    }

    private Specification<JobApplication> ownedBy(Long userId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("user").get("id"), userId);
    }

    private Specification<JobApplication> hasStatus(ApplicationStatus status) {
        return status == null
                ? null
                : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status);
    }

    private Specification<JobApplication> hasEmploymentType(EmploymentType employmentType) {
        return employmentType == null
                ? null
                : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("employmentType"), employmentType);
    }

    private Specification<JobApplication> matchesSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }

        String pattern = "%" + escapeLike(search.trim().toLowerCase()) + "%";
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("company")), pattern, '\\'));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("jobTitle")), pattern, '\\'));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("location")), pattern, '\\'));
            return criteriaBuilder.or(predicates.toArray(Predicate[]::new));
        };
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private String normalizeCurrency(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
