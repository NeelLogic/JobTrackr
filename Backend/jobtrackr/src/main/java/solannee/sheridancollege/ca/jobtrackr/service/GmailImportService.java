package solannee.sheridancollege.ca.jobtrackr.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solannee.sheridancollege.ca.jobtrackr.config.GmailImportProperties;
import solannee.sheridancollege.ca.jobtrackr.dto.application.ApplicationRequest;
import solannee.sheridancollege.ca.jobtrackr.dto.application.ApplicationResponse;
import solannee.sheridancollege.ca.jobtrackr.dto.integration.GmailImportCandidateResponse;
import solannee.sheridancollege.ca.jobtrackr.dto.integration.GmailImportScanResponse;
import solannee.sheridancollege.ca.jobtrackr.exception.ConflictException;
import solannee.sheridancollege.ca.jobtrackr.exception.ResourceNotFoundException;
import solannee.sheridancollege.ca.jobtrackr.integration.gmail.GmailApplicationEmailParser;
import solannee.sheridancollege.ca.jobtrackr.integration.gmail.GmailMailboxClient;
import solannee.sheridancollege.ca.jobtrackr.integration.gmail.GmailMessage;
import solannee.sheridancollege.ca.jobtrackr.model.GmailImportCandidate;
import solannee.sheridancollege.ca.jobtrackr.model.GmailImportState;
import solannee.sheridancollege.ca.jobtrackr.model.User;
import solannee.sheridancollege.ca.jobtrackr.repository.GmailImportCandidateRepository;
import solannee.sheridancollege.ca.jobtrackr.repository.JobApplicationRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GmailImportService {

    private final GmailImportProperties properties;
    private final GmailIntegrationService gmailIntegration;
    private final GmailConnectionStore connectionStore;
    private final GmailMailboxClient mailboxClient;
    private final GmailApplicationEmailParser parser;
    private final GmailImportCandidateStore candidateStore;
    private final GmailImportCandidateRepository candidates;
    private final JobApplicationService applications;
    private final JobApplicationRepository applicationRepository;

    public GmailImportScanResponse scan(User user) {
        String accessToken = gmailIntegration.accessToken(user);
        List<GmailMessage> messages = mailboxClient.listMessages(
                accessToken,
                searchQuery(),
                properties.maxMessages()
        );

        List<GmailImportCandidateStore.DetectedMessage> detected = new ArrayList<>();
        for (GmailMessage message : messages) {
            parser.parse(message).ifPresent(parsed -> detected.add(
                    new GmailImportCandidateStore.DetectedMessage(message, parsed)));
        }

        int added = candidateStore.saveNew(
                user,
                detected,
                messageId -> messageHash(user.getId(), messageId)
        );
        connectionStore.markSynced(user.getId(), Instant.now());
        List<GmailImportCandidateResponse> pending = candidateStore.pending(user.getId());
        return new GmailImportScanResponse(
                messages.size(),
                detected.size(),
                added,
                detected.size() - added,
                pending
        );
    }

    public List<GmailImportCandidateResponse> pending(User user) {
        return candidateStore.pending(user.getId());
    }

    @Transactional
    public ApplicationResponse importCandidate(
            User user,
            Long candidateId,
            ApplicationRequest request
    ) {
        GmailImportCandidate candidate = findPendingOwned(user.getId(), candidateId);
        ApplicationResponse application = applications.create(user, request);
        candidate.setImportedApplication(applicationRepository.getReferenceById(application.id()));
        candidate.setCandidateState(GmailImportState.IMPORTED);
        return application;
    }

    @Transactional
    public void dismiss(User user, Long candidateId) {
        GmailImportCandidate candidate = findPendingOwned(user.getId(), candidateId);
        candidate.setCandidateState(GmailImportState.DISMISSED);
    }

    private GmailImportCandidate findPendingOwned(Long userId, Long candidateId) {
        GmailImportCandidate candidate = candidates.findOwnedForUpdate(candidateId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Gmail import candidate not found"));
        if (candidate.getCandidateState() != GmailImportState.PENDING) {
            throw new ConflictException("Gmail import candidate has already been reviewed");
        }
        return candidate;
    }

    private String searchQuery() {
        return "newer_than:%dd -in:spam -in:trash "
                .formatted(properties.lookbackDays())
                + "{subject:application subject:applied subject:interview "
                + "subject:assessment subject:offer subject:rejected "
                + "from:myworkdayjobs.com}";
    }

    private String messageHash(Long userId, String messageId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = "jobtrackr:gmail-message:" + userId + ":" + messageId;
            return HexFormat.of().formatHex(
                    digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
