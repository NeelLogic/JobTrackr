package solannee.sheridancollege.ca.jobtrackr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.auth.otp")
public record AuthOtpProperties(
        Duration ttl,
        Duration resendCooldown,
        int maxAttempts,
        int length,
        String pepper
) {
    public AuthOtpProperties {
        ttl = ttl == null ? Duration.ofMinutes(10) : ttl;
        resendCooldown = resendCooldown == null ? Duration.ofMinutes(1) : resendCooldown;
        maxAttempts = maxAttempts <= 0 ? 5 : maxAttempts;
        length = length <= 0 ? 6 : length;
        pepper = pepper == null ? "" : pepper.trim();
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("OTP lifetime must be positive");
        }
        if (resendCooldown.isNegative()) {
            throw new IllegalArgumentException("OTP resend cooldown cannot be negative");
        }
        if (length < 6 || length > 8) {
            throw new IllegalArgumentException("OTP length must be between 6 and 8 digits");
        }
        if (pepper.length() < 32) {
            throw new IllegalArgumentException("OTP pepper must contain at least 32 characters");
        }
    }
}
