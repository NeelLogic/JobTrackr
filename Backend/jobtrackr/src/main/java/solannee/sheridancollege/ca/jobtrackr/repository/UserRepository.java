package solannee.sheridancollege.ca.jobtrackr.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import solannee.sheridancollege.ca.jobtrackr.model.User;
import java.util.Optional;
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
