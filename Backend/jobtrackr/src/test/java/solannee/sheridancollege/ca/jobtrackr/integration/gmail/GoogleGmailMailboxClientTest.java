package solannee.sheridancollege.ca.jobtrackr.integration.gmail;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleGmailMailboxClientTest {

    @Test
    void encodesGmailSearchGroupsAsQueryDataInsteadOfUriTemplates() {
        URI uri = GoogleGmailMailboxClient.listUri(
                "newer_than:180d {subject:application subject:interview}",
                100
        );

        assertThat(uri.toASCIIString())
                .contains("q=newer_than:180d%20%7Bsubject:application%20subject:interview%7D")
                .endsWith("&maxResults=100")
                .doesNotContain("{", "}");
    }
}
