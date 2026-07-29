package solannee.sheridancollege.ca.jobtrackr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import solannee.sheridancollege.ca.jobtrackr.model.AuthProvider;
import solannee.sheridancollege.ca.jobtrackr.model.UserIdentity;

import java.util.List;
import java.util.Optional;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, Long> {

    Optional<UserIdentity> findByProviderAndProviderSubject(AuthProvider provider, String providerSubject);

    boolean existsByUserIdAndProvider(Long userId, AuthProvider provider);

    List<UserIdentity> findAllByUserIdOrderByCreatedAtAsc(Long userId);
}
