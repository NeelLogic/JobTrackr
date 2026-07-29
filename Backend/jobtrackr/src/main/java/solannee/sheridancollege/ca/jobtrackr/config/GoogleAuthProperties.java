package solannee.sheridancollege.ca.jobtrackr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.google")
public record GoogleAuthProperties(String clientId) {

    public GoogleAuthProperties {
        clientId = clientId == null ? "" : clientId.trim();
    }

    public boolean enabled() {
        return !clientId.isBlank();
    }
}
