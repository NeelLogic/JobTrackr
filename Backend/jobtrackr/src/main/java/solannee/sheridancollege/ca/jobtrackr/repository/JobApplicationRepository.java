package solannee.sheridancollege.ca.jobtrackr.repository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.jpa.domain.Specification;
import solannee.sheridancollege.ca.jobtrackr.model.*;
import java.time.LocalDate;
import java.util.*;
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long>, JpaSpecificationExecutor<JobApplication> {
    Optional<JobApplication> findByIdAndUserId(Long id, Long userId);
    long countByUserId(Long userId);
    long countByUserIdAndStatus(Long userId, ApplicationStatus status);
    long countByUserIdAndApplicationDateBetween(Long userId, LocalDate from, LocalDate to);
    List<JobApplication> findTop5ByUserIdOrderByUpdatedAtDesc(Long userId);
}
