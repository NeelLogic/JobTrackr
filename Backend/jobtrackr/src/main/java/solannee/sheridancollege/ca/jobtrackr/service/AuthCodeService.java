package solannee.sheridancollege.ca.jobtrackr.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solannee.sheridancollege.ca.jobtrackr.config.AuthOtpProperties;
import solannee.sheridancollege.ca.jobtrackr.exception.IntegrationUnavailableException;
import solannee.sheridancollege.ca.jobtrackr.exception.InvalidRequestException;
import solannee.sheridancollege.ca.jobtrackr.model.AuthCodePurpose;
import solannee.sheridancollege.ca.jobtrackr.model.AuthOneTimeCode;
import solannee.sheridancollege.ca.jobtrackr.model.User;
import solannee.sheridancollege.ca.jobtrackr.repository.AuthOneTimeCodeRepository;
import solannee.sheridancollege.ca.jobtrackr.repository.UserRepository;
import solannee.sheridancollege.ca.jobtrackr.security.OtpHashingService;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthCodeService {

    private static final String INVALID_CODE = "The code is invalid or has expired";

    private final AuthOneTimeCodeRepository codes;
    private final UserRepository users;
    private final AuthEmailSender emailSender;
    private final OtpHashingService hashing;
    private final AuthOtpProperties properties;
    private final SecureRandom random;
    private final Clock clock;

    @Transactional
    public void sendEmailVerification(User user) {
        requireEmailDelivery();
        if (!user.isEmailVerified()) {
            issue(user, AuthCodePurpose.EMAIL_VERIFICATION);
        }
    }

    @Transactional
    public void resendEmailVerification(String rawEmail) {
        requireEmailDelivery();
        users.findByEmailIgnoreCase(normalizeEmail(rawEmail))
                .filter(user -> user.getPasswordHash() != null && !user.isEmailVerified())
                .ifPresent(user -> issue(user, AuthCodePurpose.EMAIL_VERIFICATION));
    }

    @Transactional(noRollbackFor = InvalidRequestException.class)
    public User verifyEmail(String rawEmail, String code) {
        User user = users.findByEmailIgnoreCase(normalizeEmail(rawEmail))
                .filter(candidate -> !candidate.isEmailVerified())
                .orElseThrow(() -> new InvalidRequestException(INVALID_CODE));
        consumeValidCode(user, AuthCodePurpose.EMAIL_VERIFICATION, code);
        user.setEmailVerified(true);
        return users.save(user);
    }

    @Transactional
    public void requestPasswordReset(String rawEmail) {
        requireEmailDelivery();
        users.findByEmailIgnoreCase(normalizeEmail(rawEmail))
                .filter(User::isEmailVerified)
                .filter(user -> user.getPasswordHash() != null)
                .ifPresent(user -> issue(user, AuthCodePurpose.PASSWORD_RESET));
    }

    @Transactional(noRollbackFor = InvalidRequestException.class)
    public void resetPassword(String rawEmail, String code, String passwordHash) {
        User user = users.findByEmailIgnoreCase(normalizeEmail(rawEmail))
                .filter(User::isEmailVerified)
                .filter(candidate -> candidate.getPasswordHash() != null)
                .orElseThrow(() -> new InvalidRequestException(INVALID_CODE));
        consumeValidCode(user, AuthCodePurpose.PASSWORD_RESET, code);
        user.setPasswordHash(passwordHash);
        users.save(user);
    }

    private void issue(User user, AuthCodePurpose purpose) {
        Instant now = clock.instant();
        boolean coolingDown = codes.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(
                        user.getId(), purpose)
                .map(latest -> latest.getCreatedAt().isAfter(now.minus(properties.resendCooldown())))
                .orElse(false);
        if (coolingDown) {
            return;
        }

        codes.consumeActiveCodes(user.getId(), purpose, now);
        String code = generateCode();
        AuthOneTimeCode oneTimeCode = new AuthOneTimeCode();
        oneTimeCode.setUser(user);
        oneTimeCode.setPurpose(purpose);
        oneTimeCode.setCodeHash(hashing.hash(user.getId(), purpose, code));
        oneTimeCode.setExpiresAt(now.plus(properties.ttl()));
        oneTimeCode.setCreatedAt(now);
        codes.save(oneTimeCode);

        if (purpose == AuthCodePurpose.EMAIL_VERIFICATION) {
            emailSender.sendEmailVerification(user.getEmail(), code, properties.ttl());
        } else {
            emailSender.sendPasswordReset(user.getEmail(), code, properties.ttl());
        }
    }

    private void consumeValidCode(User user, AuthCodePurpose purpose, String rawCode) {
        Instant now = clock.instant();
        AuthOneTimeCode code = codes
                .findFirstByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                        user.getId(), purpose)
                .orElseThrow(() -> new InvalidRequestException(INVALID_CODE));

        if (!code.getExpiresAt().isAfter(now)
                || code.getFailedAttempts() >= properties.maxAttempts()) {
            code.setConsumedAt(now);
            codes.save(code);
            throw new InvalidRequestException(INVALID_CODE);
        }

        if (!hashing.matches(user.getId(), purpose, rawCode.trim(), code.getCodeHash())) {
            code.setFailedAttempts(code.getFailedAttempts() + 1);
            if (code.getFailedAttempts() >= properties.maxAttempts()) {
                code.setConsumedAt(now);
            }
            codes.save(code);
            throw new InvalidRequestException(INVALID_CODE);
        }

        code.setConsumedAt(now);
        codes.save(code);
    }

    private String generateCode() {
        int bound = (int) Math.pow(10, properties.length());
        return String.format(Locale.ROOT, "%0" + properties.length() + "d", random.nextInt(bound));
    }

    private void requireEmailDelivery() {
        if (!emailSender.isAvailable()) {
            throw new IntegrationUnavailableException("Email delivery is temporarily unavailable");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
