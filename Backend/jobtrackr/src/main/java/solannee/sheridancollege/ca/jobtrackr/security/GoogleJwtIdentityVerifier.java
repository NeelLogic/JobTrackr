package solannee.sheridancollege.ca.jobtrackr.security;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import solannee.sheridancollege.ca.jobtrackr.config.GoogleAuthProperties;
import solannee.sheridancollege.ca.jobtrackr.exception.GoogleAuthenticationException;

import java.util.Locale;

@Component
public class GoogleJwtIdentityVerifier implements GoogleIdentityVerifier {

    private static final String GOOGLE_ISSUER = "https://accounts.google.com";
    private static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";

    private final GoogleAuthProperties properties;
    private final JwtDecoder decoder;

    public GoogleJwtIdentityVerifier(GoogleAuthProperties properties) {
        this.properties = properties;
        this.decoder = properties.enabled() ? createDecoder(properties.clientId()) : null;
    }

    @Override
    public VerifiedGoogleIdentity verify(String credential) {
        if (decoder == null) {
            throw new GoogleAuthenticationException("Google sign-in is not configured");
        }

        try {
            Jwt jwt = decoder.decode(credential);
            String subject = required(jwt.getSubject());
            String email = required(jwt.getClaimAsString("email")).toLowerCase(Locale.ROOT);
            if (!Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))) {
                throw new GoogleAuthenticationException("Google account email is not verified");
            }

            String name = jwt.getClaimAsString("name");
            if (name == null || name.isBlank()) {
                int separator = email.indexOf('@');
                name = separator > 0 ? email.substring(0, separator) : email;
            }
            return new VerifiedGoogleIdentity(subject, email, name.trim());
        } catch (JwtException | IllegalArgumentException exception) {
            throw new GoogleAuthenticationException("Google credential is invalid or expired", exception);
        }
    }

    private JwtDecoder createDecoder(String clientId) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_SET_URI).build();
        OAuth2TokenValidator<Jwt> issuer = JwtValidators.createDefaultWithIssuer(GOOGLE_ISSUER);
        OAuth2TokenValidator<Jwt> audience = token -> token.getAudience().contains(clientId)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_token", "Google token audience does not match JobTrackr", null
                ));
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuer, audience));
        return jwtDecoder;
    }

    private String required(String value) {
        if (value == null || value.isBlank()) {
            throw new GoogleAuthenticationException("Google credential is missing required account information");
        }
        return value.trim();
    }
}
