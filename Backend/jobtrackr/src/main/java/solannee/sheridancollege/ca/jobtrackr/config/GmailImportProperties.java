package solannee.sheridancollege.ca.jobtrackr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.integrations.gmail.import")
public record GmailImportProperties(
        Integer lookbackDays,
        Integer maxMessages
) {

    private static final int DEFAULT_LOOKBACK_DAYS = 180;
    private static final int DEFAULT_MAX_MESSAGES = 100;

    public GmailImportProperties {
        lookbackDays = bounded(lookbackDays, DEFAULT_LOOKBACK_DAYS, 1, 365);
        maxMessages = bounded(maxMessages, DEFAULT_MAX_MESSAGES, 1, 100);
    }

    private static int bounded(Integer value, int fallback, int minimum, int maximum) {
        int resolved = value == null ? fallback : value;
        return Math.max(minimum, Math.min(resolved, maximum));
    }
}
