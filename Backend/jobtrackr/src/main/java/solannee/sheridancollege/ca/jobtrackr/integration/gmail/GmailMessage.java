package solannee.sheridancollege.ca.jobtrackr.integration.gmail;

import java.time.Instant;

public record GmailMessage(
        String id,
        String subject,
        String sender,
        Instant receivedAt,
        String content
) {
}
