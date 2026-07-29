package solannee.sheridancollege.ca.jobtrackr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import solannee.sheridancollege.ca.jobtrackr.model.GmailConnection;

import java.util.Optional;

public interface GmailConnectionRepository extends JpaRepository<GmailConnection, Long> {

    Optional<GmailConnection> findByUserId(Long userId);

    boolean existsByGoogleEmailIgnoreCaseAndUserIdNot(String googleEmail, Long userId);
}
