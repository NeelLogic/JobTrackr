package solannee.sheridancollege.ca.jobtrackr.integration.gmail;

import java.util.List;

public interface GmailMailboxClient {

    List<GmailMessage> listMessages(String accessToken, String query, int maxResults);
}
