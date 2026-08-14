package solannee.sheridancollege.ca.jobtrackr.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import solannee.sheridancollege.ca.jobtrackr.config.AuthOtpProperties;
import solannee.sheridancollege.ca.jobtrackr.exception.InvalidRequestException;
import solannee.sheridancollege.ca.jobtrackr.model.AuthCodePurpose;
import solannee.sheridancollege.ca.jobtrackr.model.AuthOneTimeCode;
import solannee.sheridancollege.ca.jobtrackr.model.User;
import solannee.sheridancollege.ca.jobtrackr.repository.AuthOneTimeCodeRepository;
import solannee.sheridancollege.ca.jobtrackr.repository.UserRepository;
import solannee.sheridancollege.ca.jobtrackr.security.OtpHashingService;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthCodeServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-14T12:00:00Z");

    @Mock AuthOneTimeCodeRepository codeRepository;
    @Mock UserRepository userRepository;
    @Mock AuthEmailSender emailSender;
    @Mock SecureRandom random;

    private AuthCodeService service;
    private OtpHashingService hashing;
    private AuthOtpProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AuthOtpProperties(
                Duration.ofMinutes(10),
                Duration.ofMinutes(1),
                5,
                6,
                "test-otp-pepper-that-is-longer-than-thirty-two-characters"
        );
        hashing = new OtpHashingService(properties);
        service = new AuthCodeService(
                codeRepository,
                userRepository,
                emailSender,
                hashing,
                properties,
                random,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void verificationCodesAreHashedBeforeStorageAndDelivered() {
        User user = user(false);
        when(emailSender.isAvailable()).thenReturn(true);
        when(codeRepository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(
                7L, AuthCodePurpose.EMAIL_VERIFICATION)).thenReturn(Optional.empty());
        when(random.nextInt(1_000_000)).thenReturn(123456);

        service.sendEmailVerification(user);

        ArgumentCaptor<AuthOneTimeCode> stored = ArgumentCaptor.forClass(AuthOneTimeCode.class);
        verify(codeRepository).save(stored.capture());
        assertThat(stored.getValue().getCodeHash()).hasSize(64).doesNotContain("123456");
        assertThat(stored.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(10)));
        verify(emailSender).sendEmailVerification(
                "user@example.com", "123456", Duration.ofMinutes(10));
    }

    @Test
    void validVerificationCodeIsSingleUseAndConfirmsTheUser() {
        User user = user(false);
        AuthOneTimeCode code = code(user, AuthCodePurpose.EMAIL_VERIFICATION, "123456");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(codeRepository.findFirstByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                7L, AuthCodePurpose.EMAIL_VERIFICATION)).thenReturn(Optional.of(code));
        when(userRepository.save(user)).thenReturn(user);

        User verified = service.verifyEmail(" User@Example.com ", "123456");

        assertThat(verified.isEmailVerified()).isTrue();
        assertThat(code.getConsumedAt()).isEqualTo(NOW);
        verify(codeRepository).save(code);
    }

    @Test
    void invalidAttemptsConsumeTheCodeAtTheConfiguredLimit() {
        User user = user(true);
        AuthOneTimeCode code = code(user, AuthCodePurpose.PASSWORD_RESET, "123456");
        code.setFailedAttempts(4);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(codeRepository.findFirstByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                7L, AuthCodePurpose.PASSWORD_RESET)).thenReturn(Optional.of(code));

        assertThatThrownBy(() -> service.resetPassword(
                "user@example.com", "999999", "new-password-hash"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("invalid or has expired");

        assertThat(code.getFailedAttempts()).isEqualTo(5);
        assertThat(code.getConsumedAt()).isEqualTo(NOW);
    }

    @Test
    void expiredCodesAreRejectedAndConsumed() {
        User user = user(true);
        AuthOneTimeCode code = code(user, AuthCodePurpose.PASSWORD_RESET, "123456");
        code.setExpiresAt(NOW);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(codeRepository.findFirstByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                7L, AuthCodePurpose.PASSWORD_RESET)).thenReturn(Optional.of(code));

        assertThatThrownBy(() -> service.resetPassword(
                "user@example.com", "123456", "new-password-hash"))
                .isInstanceOf(InvalidRequestException.class);

        assertThat(code.getConsumedAt()).isEqualTo(NOW);
    }

    @Test
    void resendCooldownDoesNotCreateOrEmailAnotherCode() {
        User user = user(false);
        AuthOneTimeCode latest = code(user, AuthCodePurpose.EMAIL_VERIFICATION, "123456");
        latest.setCreatedAt(NOW.minusSeconds(20));
        when(emailSender.isAvailable()).thenReturn(true);
        when(codeRepository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(
                7L, AuthCodePurpose.EMAIL_VERIFICATION)).thenReturn(Optional.of(latest));

        service.sendEmailVerification(user);

        verify(codeRepository, never()).save(any());
        verify(emailSender, never()).sendEmailVerification(any(), any(), any());
    }

    private User user(boolean verified) {
        User user = new User();
        user.setId(7L);
        user.setName("User");
        user.setEmail("user@example.com");
        user.setPasswordHash("hashed-password");
        user.setEmailVerified(verified);
        return user;
    }

    private AuthOneTimeCode code(User user, AuthCodePurpose purpose, String rawCode) {
        AuthOneTimeCode code = new AuthOneTimeCode();
        code.setId(3L);
        code.setUser(user);
        code.setPurpose(purpose);
        code.setCodeHash(hashing.hash(user.getId(), purpose, rawCode));
        code.setCreatedAt(NOW.minusSeconds(30));
        code.setExpiresAt(NOW.plusSeconds(300));
        return code;
    }
}
