package solannee.sheridancollege.ca.jobtrackr.dto.integration;

import java.util.List;

public record GmailImportScanResponse(
        int messagesScanned,
        int matchesDetected,
        int candidatesAdded,
        int duplicatesSkipped,
        List<GmailImportCandidateResponse> candidates
) {
}
