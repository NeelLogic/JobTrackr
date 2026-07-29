package solannee.sheridancollege.ca.jobtrackr.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import solannee.sheridancollege.ca.jobtrackr.config.GmailOAuthProperties;
import solannee.sheridancollege.ca.jobtrackr.dto.integration.GmailAuthorizationResponse;
import solannee.sheridancollege.ca.jobtrackr.dto.integration.GmailConnectionResponse;
import solannee.sheridancollege.ca.jobtrackr.exception.IntegrationUnavailableException;
import solannee.sheridancollege.ca.jobtrackr.exception.InvalidRequestException;
import solannee.sheridancollege.ca.jobtrackr.integration.gmail.GmailOAuthClient;
import solannee.sheridancollege.ca.jobtrackr.integration.gmail.GoogleGmailProfile;
import solannee.sheridancollege.ca.jobtrackr.integration.gmail.GoogleOAuthTokens;
import solannee.sheridancollege.ca.jobtrackr.model.User;

import java.net.URI;
import java.time.Instant;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class GmailIntegrationService {

    public static final String GMAIL_READONLY_SCOPE =
            "https://www.googleapis.com/auth/gmail.readonly";
    private static final URI GOOGLE_AUTHORIZATION_ENDPOINT =
            URI.create("https://accounts.google.com/o/oauth2/v2/auth");
    private static final long EXPIRY_SAFETY_SECONDS = 30;

    private final GmailOAuthProperties properties;
    private final OAuthStateService oauthStates;
    private final GmailConnectionStore connectionStore;
    private final GmailOAuthClient gmailClient;

    public GmailConnectionResponse status(User user) {
        return connectionStore.status(user.getId(), properties.enabled())
                .orElseGet(() -> new GmailConnectionResponse(
                        properties.enabled(), false, null, null, null));
    }

    public GmailAuthorizationResponse beginAuthorization(User user) {
        requireConfigured();
        String state = oauthStates.create(user);
        String authorizationUrl = UriComponentsBuilder
                .fromUri(GOOGLE_AUTHORIZATION_ENDPOINT)
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", GMAIL_READONLY_SCOPE)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("include_granted_scopes", "true")
                .queryParam("login_hint", user.getEmail())
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
        return new GmailAuthorizationResponse(authorizationUrl);
    }

    public void completeAuthorization(String code, String state) {
        requireConfigured();
        OAuthStateService.OAuthStateOwner owner = oauthStates.consume(state);
        if (code == null || code.isBlank()) {
            throw new InvalidRequestException("Google authorization code is required");
        }

        GoogleOAuthTokens tokens = gmailClient.exchangeAuthorizationCode(code);
        GoogleGmailProfile profile = gmailClient.getProfile(tokens.accessToken());
        if (!owner.email().equalsIgnoreCase(profile.emailAddress())) {
            bestEffortRevoke(tokens);
            throw new InvalidRequestException(
                    "Connected Gmail address must match your JobTrackr account email");
        }
        if (!hasRequiredScope(tokens.scopes())) {
            bestEffortRevoke(tokens);
            throw new InvalidRequestException("Gmail read permission was not granted");
        }

        connectionStore.save(
                owner.userId(),
                profile.emailAddress(),
                tokens.accessToken(),
                tokens.refreshToken(),
                expiresAt(tokens.expiresInSeconds()),
                tokens.scopes()
        );
    }

    public void cancelAuthorization(String state) {
        oauthStates.consume(state);
    }

    public void disconnect(User user) {
        connectionStore.remove(user.getId()).ifPresent(tokens -> {
            try {
                gmailClient.revoke(tokens.refreshToken());
            } catch (RuntimeException ignored) {
                // Local removal is authoritative. A failed remote revoke must not restore credentials.
            }
        });
    }

    public String accessToken(User user) {
        requireConfigured();
        GmailConnectionStore.GmailTokenSnapshot tokens = connectionStore.findTokens(user.getId())
                .orElseThrow(() -> new InvalidRequestException("Gmail is not connected"));
        if (tokens.accessTokenExpiresAt().isAfter(Instant.now().plusSeconds(EXPIRY_SAFETY_SECONDS))) {
            return tokens.accessToken();
        }

        GoogleOAuthTokens refreshed = gmailClient.refreshAccessToken(tokens.refreshToken());
        connectionStore.updateAccessToken(
                user.getId(),
                refreshed.accessToken(),
                refreshed.refreshToken(),
                expiresAt(refreshed.expiresInSeconds()),
                refreshed.scopes()
        );
        return refreshed.accessToken();
    }

    public URI frontendResultUri(String result) {
        return UriComponentsBuilder.fromUri(properties.frontendCallbackUrl())
                .replaceQueryParam("gmail", result)
                .build()
                .encode()
                .toUri();
    }

    private boolean hasRequiredScope(String scopes) {
        if (scopes == null) {
            return false;
        }
        return Arrays.stream(scopes.trim().split("\\s+"))
                .anyMatch(GMAIL_READONLY_SCOPE::equals);
    }

    private Instant expiresAt(long expiresInSeconds) {
        return Instant.now().plusSeconds(Math.max(expiresInSeconds, 60));
    }

    private void bestEffortRevoke(GoogleOAuthTokens tokens) {
        String token = tokens.refreshToken() == null || tokens.refreshToken().isBlank()
                ? tokens.accessToken()
                : tokens.refreshToken();
        try {
            gmailClient.revoke(token);
        } catch (RuntimeException ignored) {
            // The connection was rejected locally, regardless of Google's revoke response.
        }
    }

    private void requireConfigured() {
        if (!properties.enabled()) {
            throw new IntegrationUnavailableException(
                    "Gmail integration is not configured for this environment");
        }
    }
}
