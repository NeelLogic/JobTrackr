package solannee.sheridancollege.ca.jobtrackr.integration.gmail;

public interface GmailOAuthClient {

    GoogleOAuthTokens exchangeAuthorizationCode(String code);

    GoogleOAuthTokens refreshAccessToken(String refreshToken);

    GoogleGmailProfile getProfile(String accessToken);

    void revoke(String token);
}
