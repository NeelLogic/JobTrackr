package solannee.sheridancollege.ca.jobtrackr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import solannee.sheridancollege.ca.jobtrackr.model.ApplicationStatusHistory;

import java.util.List;

public interface ApplicationStatusHistoryRepository
        extends JpaRepository<ApplicationStatusHistory, Long> {

    List<ApplicationStatusHistory> findAllByUserIdOrderByChangedAtAsc(Long userId);
}
