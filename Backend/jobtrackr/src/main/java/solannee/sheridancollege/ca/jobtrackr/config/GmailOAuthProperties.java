package solannee.sheridancollege.ca.jobtrackr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.Base64;

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
                && hasValidEncryptionKey()
                && redirectUri != null
                && frontendCallbackUrl != null;
    }

    private boolean hasValidEncryptionKey() {
        try {
            return Base64.getDecoder().decode(tokenEncryptionKey).length == 32;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
