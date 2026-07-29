package solannee.sheridancollege.ca.jobtrackr.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import solannee.sheridancollege.ca.jobtrackr.model.GmailImportCandidate;
import solannee.sheridancollege.ca.jobtrackr.model.GmailImportState;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface GmailImportCandidateRepository
        extends JpaRepository<GmailImportCandidate, Long> {

    List<GmailImportCandidate> findAllByUserIdAndCandidateStateOrderByReceivedAtDesc(
            Long userId,
            GmailImportState candidateState
    );

    @Query("""
            select candidate.messageIdHash
            from GmailImportCandidate candidate
            where candidate.user.id = :userId
              and candidate.messageIdHash in :messageHashes
            """)
    Set<String> findExistingMessageHashes(
            @Param("userId") Long userId,
            @Param("messageHashes") Set<String> messageHashes
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select candidate from GmailImportCandidate candidate
            where candidate.id = :candidateId and candidate.user.id = :userId
            """)
    Optional<GmailImportCandidate> findOwnedForUpdate(
            @Param("candidateId") Long candidateId,
            @Param("userId") Long userId
    );
}
