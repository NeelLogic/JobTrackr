package solannee.sheridancollege.ca.jobtrackr.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solannee.sheridancollege.ca.jobtrackr.dto.integration.GmailImportCandidateResponse;
import solannee.sheridancollege.ca.jobtrackr.integration.gmail.GmailMessage;
import solannee.sheridancollege.ca.jobtrackr.integration.gmail.ParsedGmailApplication;
import solannee.sheridancollege.ca.jobtrackr.model.GmailImportCandidate;
import solannee.sheridancollege.ca.jobtrackr.model.GmailImportState;
import solannee.sheridancollege.ca.jobtrackr.model.User;
import solannee.sheridancollege.ca.jobtrackr.repository.GmailImportCandidateRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GmailImportCandidateStore {

    private static final int SUBJECT_MAX = 500;
    private static final int SENDER_MAX = 500;

    private final GmailImportCandidateRepository candidates;

    @Transactional
    public int saveNew(
            User user,
            List<DetectedMessage> detected,
            Function<String, String> messageHasher
    ) {
        Set<String> hashes = detected.stream()
                .map(item -> messageHasher.apply(item.message().id()))
                .collect(Collectors.toSet());
        Set<String> existing = hashes.isEmpty()
                ? Set.of()
                : candidates.findExistingMessageHashes(user.getId(), hashes);

        Set<String> knownHashes = new HashSet<>(existing);
        List<GmailImportCandidate> additions = new ArrayList<>();
        for (DetectedMessage item : detected) {
            String hash = messageHasher.apply(item.message().id());
            if (!knownHashes.add(hash)) {
                continue;
            }
            additions.add(toEntity(user, hash, item));
        }
        candidates.saveAll(additions);
        return additions.size();
    }

    @Transactional(readOnly = true)
    public List<GmailImportCandidateResponse> pending(Long userId) {
        return candidates.findAllByUserIdAndCandidateStateOrderByReceivedAtDesc(
                        userId, GmailImportState.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    GmailImportCandidateResponse toResponse(GmailImportCandidate candidate) {
        return new GmailImportCandidateResponse(
                candidate.getId(),
                candidate.getProvider(),
                candidate.getConfidence(),
                candidate.getCompany(),
                candidate.getJobTitle(),
                candidate.getLocation(),
                candidate.getJobUrl(),
                candidate.getApplicationDate(),
                candidate.getStatus(),
                candidate.getEmploymentType(),
                candidate.getSourceSubject(),
                candidate.getSourceSender(),
                candidate.getReceivedAt(),
                candidate.getCandidateState(),
                candidate.getImportedApplication() == null
                        ? null
                        : candidate.getImportedApplication().getId(),
                candidate.getDetectedAt()
        );
    }

    private GmailImportCandidate toEntity(
            User user,
            String hash,
            DetectedMessage item
    ) {
        GmailMessage message = item.message();
        ParsedGmailApplication parsed = item.application();
        GmailImportCandidate candidate = new GmailImportCandidate();
        candidate.setUser(user);
        candidate.setMessageIdHash(hash);
        candidate.setProvider(parsed.provider());
        candidate.setConfidence(parsed.confidence());
        candidate.setCompany(parsed.company());
        candidate.setJobTitle(parsed.jobTitle());
        candidate.setLocation(parsed.location());
        candidate.setJobUrl(parsed.jobUrl());
        candidate.setApplicationDate(parsed.applicationDate());
        candidate.setStatus(parsed.status());
        candidate.setEmploymentType(parsed.employmentType());
        candidate.setSourceSubject(truncate(message.subject(), SUBJECT_MAX, "(No subject)"));
        candidate.setSourceSender(truncate(message.sender(), SENDER_MAX, "(Unknown sender)"));
        candidate.setReceivedAt(message.receivedAt());
        candidate.setCandidateState(GmailImportState.PENDING);
        return candidate;
    }

    private String truncate(String value, int maximum, String fallback) {
        String resolved = value == null || value.isBlank() ? fallback : value.trim();
        return resolved.length() <= maximum ? resolved : resolved.substring(0, maximum);
    }

    public record DetectedMessage(
            GmailMessage message,
            ParsedGmailApplication application
    ) {
    }
}
