package solannee.sheridancollege.ca.jobtrackr.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import solannee.sheridancollege.ca.jobtrackr.model.OAuthState;

import java.time.Instant;
import java.util.Optional;

public interface OAuthStateRepository extends JpaRepository<OAuthState, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select state from OAuthState state
            join fetch state.user
            where state.provider = :provider and state.stateHash = :stateHash
            """)
    Optional<OAuthState> findForUpdate(
            @Param("provider") String provider,
            @Param("stateHash") String stateHash
    );

    long deleteByExpiresAtBefore(Instant cutoff);
}
