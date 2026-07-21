package solannee.sheridancollege.ca.jobtrackr.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import solannee.sheridancollege.ca.jobtrackr.dto.application.ApplicationRequest;
import solannee.sheridancollege.ca.jobtrackr.dto.application.ApplicationResponse;
import solannee.sheridancollege.ca.jobtrackr.dto.application.PageResponse;
import solannee.sheridancollege.ca.jobtrackr.model.ApplicationStatus;
import solannee.sheridancollege.ca.jobtrackr.model.EmploymentType;
import solannee.sheridancollege.ca.jobtrackr.model.User;
import solannee.sheridancollege.ca.jobtrackr.service.CurrentUserService;
import solannee.sheridancollege.ca.jobtrackr.service.JobApplicationService;

import java.net.URI;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@Validated
public class JobApplicationController {

    private final JobApplicationService applicationService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public PageResponse<ApplicationResponse> list(
            Authentication authentication,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) EmploymentType employmentType,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "updatedAt") String sort,
            @RequestParam(defaultValue = "desc")
            @Pattern(regexp = "(?i)asc|desc", message = "must be 'asc' or 'desc'") String direction
    ) {
        User user = currentUserService.require(authentication);
        return applicationService.list(user, search, status, employmentType, page, size, sort, direction);
    }

    @GetMapping("/{id}")
    public ApplicationResponse get(Authentication authentication, @PathVariable @Positive Long id) {
        return applicationService.get(currentUserService.require(authentication), id);
    }

    @PostMapping
    public ResponseEntity<ApplicationResponse> create(
            Authentication authentication,
            @Valid @RequestBody ApplicationRequest request
    ) {
        ApplicationResponse created = applicationService.create(currentUserService.require(authentication), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ApplicationResponse update(
            Authentication authentication,
            @PathVariable @Positive Long id,
            @Valid @RequestBody ApplicationRequest request
    ) {
        return applicationService.update(currentUserService.require(authentication), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable @Positive Long id) {
        applicationService.delete(currentUserService.require(authentication), id);
    }
}
