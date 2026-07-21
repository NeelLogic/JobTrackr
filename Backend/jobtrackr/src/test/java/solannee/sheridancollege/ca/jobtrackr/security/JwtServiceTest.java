package solannee.sheridancollege.ca.jobtrackr.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "a-secure-test-secret-with-more-than-thirty-two-bytes";

    @Test
    void generatesAndValidatesTokenForSubject() {
        JwtService service = new JwtService(SECRET, 60_000);

        String token = service.generateToken("user@example.com");

        assertThat(service.extractSubject(token)).isEqualTo("user@example.com");
        assertThat(service.getExpirationSeconds()).isEqualTo(60);
    }

    @Test
    void rejectsTokenSignedWithDifferentKey() {
        JwtService issuer = new JwtService(SECRET, 60_000);
        JwtService verifier = new JwtService("another-secure-test-secret-with-more-than-thirty-two-bytes", 60_000);

        assertThatThrownBy(() -> verifier.extractSubject(issuer.generateToken("user@example.com")))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() throws InterruptedException {
        JwtService service = new JwtService(SECRET, 1);
        String token = service.generateToken("user@example.com");
        Thread.sleep(5);

        assertThatThrownBy(() -> service.extractSubject(token)).isInstanceOf(JwtException.class);
    }
}
