package solannee.sheridancollege.ca.jobtrackr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "app.integrations.gmail")
public record GmailOAuthProperties(
        String clientId,
        String clientSecret,
        String tokenEncryptionKey,
        URI redirectUri,
        URI frontendCallbackUrl,
        Duration stateTtl
) {

    public GmailOAuthProperties {
        clientId = normalize(clientId);
        clientSecret = normalize(clientSecret);
        tokenEncryptionKey = normalize(tokenEncryptionKey);
        stateTtl = stateTtl == null ? Duration.ofMinutes(10) : stateTtl;
    }

    public boolean enabled() {
        return !clientId.isBlank()
                && !clientSecret.isBlank()
                && !tokenEncryptionKey.isBlank()
                && redirectUri != null
                && frontendCallbackUrl != null;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
