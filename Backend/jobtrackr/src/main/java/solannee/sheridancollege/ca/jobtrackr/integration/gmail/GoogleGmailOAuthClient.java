package solannee.sheridancollege.ca.jobtrackr.integration.gmail;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import solannee.sheridancollege.ca.jobtrackr.config.GmailOAuthProperties;
import solannee.sheridancollege.ca.jobtrackr.exception.ExternalServiceException;

@Component
public class GoogleGmailOAuthClient implements GmailOAuthClient {

    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String REVOKE_ENDPOINT = "https://oauth2.googleapis.com/revoke";
    private static final String PROFILE_ENDPOINT =
            "https://gmail.googleapis.com/gmail/v1/users/me/profile";

    private final GmailOAuthProperties properties;
    private final RestClient restClient;

    public GoogleGmailOAuthClient(GmailOAuthProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public GoogleOAuthTokens exchangeAuthorizationCode(String code) {
        MultiValueMap<String, String> form = clientCredentials();
        form.add("code", code);
        form.add("grant_type", "authorization_code");
        form.add("redirect_uri", properties.redirectUri().toString());
        return requestTokens(form, "Google authorization code exchange failed");
    }

    @Override
    public GoogleOAuthTokens refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> form = clientCredentials();
        form.add("refresh_token", refreshToken);
        form.add("grant_type", "refresh_token");
        return requestTokens(form, "Google access token refresh failed");
    }

    @Override
    public GoogleGmailProfile getProfile(String accessToken) {
        try {
            GmailProfilePayload payload = restClient.get()
                    .uri(PROFILE_ENDPOINT)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(GmailProfilePayload.class);
            if (payload == null || payload.emailAddress() == null || payload.emailAddress().isBlank()) {
                throw new ExternalServiceException("Google did not return a Gmail profile email");
            }
            return new GoogleGmailProfile(payload.emailAddress().trim());
        } catch (RestClientException exception) {
            throw new ExternalServiceException("Unable to verify the connected Gmail account", exception);
        }
    }

    @Override
    public void revoke(String token) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("token", token);
        try {
            restClient.post()
                    .uri(REVOKE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new ExternalServiceException("Google token revocation failed", exception);
        }
    }

    private MultiValueMap<String, String> clientCredentials() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        return form;
    }

    private GoogleOAuthTokens requestTokens(
            MultiValueMap<String, String> form,
            String errorMessage
    ) {
        try {
            GoogleTokenPayload payload = restClient.post()
                    .uri(TOKEN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(GoogleTokenPayload.class);
            if (payload == null || payload.accessToken() == null || payload.accessToken().isBlank()) {
                throw new ExternalServiceException("Google did not return an access token");
            }
            return new GoogleOAuthTokens(
                    payload.accessToken(),
                    payload.refreshToken(),
                    payload.expiresIn(),
                    payload.scope()
            );
        } catch (RestClientException exception) {
            throw new ExternalServiceException(errorMessage, exception);
        }
    }

    private record GoogleTokenPayload(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("expires_in") long expiresIn,
            String scope
    ) {
    }

    private record GmailProfilePayload(String emailAddress) {
    }
}
