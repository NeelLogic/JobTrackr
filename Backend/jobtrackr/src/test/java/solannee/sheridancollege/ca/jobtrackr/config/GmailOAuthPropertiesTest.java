package solannee.sheridancollege.ca.jobtrackr.config;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class GmailOAuthPropertiesTest {

    private static final String VALID_KEY =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final URI REDIRECT_URI =
            URI.create("http://localhost:8080/api/integrations/gmail/callback");
    private static final URI FRONTEND_URI =
            URI.create("http://localhost:4200/settings");

    @Test
    void enablesGmailOnlyWhenEveryRequiredServerSettingIsValid() {
        GmailOAuthProperties properties = properties(
                "gmail-client.apps.googleusercontent.com",
                "client-secret",
                VALID_KEY,
                REDIRECT_URI,
                FRONTEND_URI
        );

        assertThat(properties.enabled()).isTrue();
    }

    @Test
    void treatsMissingOAuthCredentialsAsAnOptionalDisabledIntegration() {
        GmailOAuthProperties properties = properties(
                "",
                "",
                VALID_KEY,
                REDIRECT_URI,
                FRONTEND_URI
        );

        assertThat(properties.enabled()).isFalse();
    }

    @Test
    void disablesGmailWhenTheEncryptionKeyCannotProvideAes256() {
        GmailOAuthProperties properties = properties(
                "gmail-client.apps.googleusercontent.com",
                "client-secret",
                "dG9vLXNob3J0",
                REDIRECT_URI,
                FRONTEND_URI
        );

        assertThat(properties.enabled()).isFalse();
    }

    private GmailOAuthProperties properties(
            String clientId,
            String clientSecret,
            String encryptionKey,
            URI redirectUri,
            URI frontendUri
    ) {
        return new GmailOAuthProperties(
                clientId,
                clientSecret,
                encryptionKey,
                redirectUri,
                frontendUri,
                Duration.ofMinutes(10)
        );
    }
}
