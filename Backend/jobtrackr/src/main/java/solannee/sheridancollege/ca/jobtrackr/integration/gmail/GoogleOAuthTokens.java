package solannee.sheridancollege.ca.jobtrackr.integration.gmail;

public record GoogleOAuthTokens(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        String scopes
) {
}
