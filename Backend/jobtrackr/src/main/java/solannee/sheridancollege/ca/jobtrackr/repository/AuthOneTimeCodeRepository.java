package solannee.sheridancollege.ca.jobtrackr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import solannee.sheridancollege.ca.jobtrackr.model.AuthCodePurpose;
import solannee.sheridancollege.ca.jobtrackr.model.AuthOneTimeCode;

import java.time.Instant;
import java.util.Optional;

public interface AuthOneTimeCodeRepository extends JpaRepository<AuthOneTimeCode, Long> {

    Optional<AuthOneTimeCode> findFirstByUserIdAndPurposeOrderByCreatedAtDesc(
            Long userId,
            AuthCodePurpose purpose
    );

    Optional<AuthOneTimeCode> findFirstByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
            Long userId,
            AuthCodePurpose purpose
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AuthOneTimeCode code
               set code.consumedAt = :consumedAt
             where code.user.id = :userId
               and code.purpose = :purpose
               and code.consumedAt is null
            """)
    int consumeActiveCodes(
            @Param("userId") Long userId,
            @Param("purpose") AuthCodePurpose purpose,
            @Param("consumedAt") Instant consumedAt
    );
}
