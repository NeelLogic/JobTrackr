package solannee.sheridancollege.ca.jobtrackr.security;

import org.junit.jupiter.api.Test;
import solannee.sheridancollege.ca.jobtrackr.config.GmailOAuthProperties;
import solannee.sheridancollege.ca.jobtrackr.exception.IntegrationUnavailableException;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenEncryptionServiceTest {

    private static final String KEY =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private final TokenEncryptionService service = new TokenEncryptionService(properties(KEY));

    @Test
    void encryptsAndDecryptsTokenForItsOwner() {
        String encrypted = service.encrypt("refresh-token", 42L);

        assertThat(encrypted).doesNotContain("refresh-token");
        assertThat(service.decrypt(encrypted, 42L)).isEqualTo("refresh-token");
    }

    @Test
    void usesANewInitializationVectorForEveryEncryption() {
        String first = service.encrypt("same-token", 42L);
        String second = service.encrypt("same-token", 42L);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void bindsEncryptedCredentialsToTheOwningUser() {
        String encrypted = service.encrypt("private-token", 42L);

        assertThatThrownBy(() -> service.decrypt(encrypted, 99L))
                .isInstanceOf(IntegrationUnavailableException.class);
    }

    @Test
    void rejectsKeysThatAreNotExactly256Bits() {
        TokenEncryptionService invalid = new TokenEncryptionService(properties("dG9vLXNob3J0"));

        assertThatThrownBy(() -> invalid.encrypt("token", 42L))
                .isInstanceOf(IntegrationUnavailableException.class)
                .hasMessageContaining("token encryption key");
    }

    private GmailOAuthProperties properties(String key) {
        return new GmailOAuthProperties(
                "client-id",
                "client-secret",
                key,
                URI.create("http://localhost/callback"),
                URI.create("http://localhost/settings"),
                Duration.ofMinutes(10)
        );
    }
}
