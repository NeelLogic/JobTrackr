package solannee.sheridancollege.ca.jobtrackr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.mail")
public record AuthMailProperties(String host, String from) {
    public AuthMailProperties {
        host = host == null ? "" : host.trim();
        from = from == null ? "" : from.trim();
    }

    public boolean enabled() {
        return !host.isBlank() && !from.isBlank();
    }
}
