package solannee.sheridancollege.ca.jobtrackr.dto.integration;

import java.time.Instant;

public record GmailConnectionResponse(
        boolean configured,
        boolean connected,
        String email,
        Instant connectedAt,
        Instant lastSyncAt
) {
}
