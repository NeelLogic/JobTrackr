package solannee.sheridancollege.ca.jobtrackr.mapper;

import org.springframework.stereotype.Component;
import solannee.sheridancollege.ca.jobtrackr.dto.application.ApplicationResponse;
import solannee.sheridancollege.ca.jobtrackr.model.JobApplication;

@Component
public class ApplicationMapper {

    public ApplicationResponse toResponse(JobApplication application) {
        return new ApplicationResponse(
                application.getId(),
                application.getCompany(),
                application.getJobTitle(),
                application.getLocation(),
                application.getJobUrl(),
                application.getApplicationDate(),
                application.getStatus(),
                application.getEmploymentType(),
                application.getSalaryMin(),
                application.getSalaryMax(),
                application.getSalaryCurrency(),
                application.getNotes(),
                application.getFollowUpDate(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }
}
