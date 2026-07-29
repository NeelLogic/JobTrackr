package solannee.sheridancollege.ca.jobtrackr.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solannee.sheridancollege.ca.jobtrackr.dto.integration.GmailConnectionResponse;
import solannee.sheridancollege.ca.jobtrackr.exception.ConflictException;
import solannee.sheridancollege.ca.jobtrackr.exception.InvalidRequestException;
import solannee.sheridancollege.ca.jobtrackr.model.GmailConnection;
import solannee.sheridancollege.ca.jobtrackr.model.User;
import solannee.sheridancollege.ca.jobtrackr.repository.GmailConnectionRepository;
import solannee.sheridancollege.ca.jobtrackr.repository.UserRepository;
import solannee.sheridancollege.ca.jobtrackr.security.TokenEncryptionService;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GmailConnectionStore {

    private final GmailConnectionRepository connections;
    private final UserRepository users;
    private final TokenEncryptionService tokenEncryption;

    @Transactional(readOnly = true)
    public Optional<GmailConnectionResponse> status(Long userId, boolean configured) {
        return connections.findByUserId(userId)
                .map(connection -> new GmailConnectionResponse(
                        configured,
                        true,
                        connection.getGoogleEmail(),
                        connection.getConnectedAt(),
                        connection.getLastSyncAt()
                ));
    }

    @Transactional
    public void save(
            Long userId,
            String googleEmail,
            String accessToken,
            String refreshToken,
            Instant accessTokenExpiresAt,
            String scopes
    ) {
        String normalizedEmail = googleEmail.trim().toLowerCase(Locale.ROOT);
        if (connections.existsByGoogleEmailIgnoreCaseAndUserIdNot(normalizedEmail, userId)) {
            throw new ConflictException("This Gmail account is already connected to another user");
        }

        GmailConnection connection = connections.findByUserId(userId).orElseGet(() -> {
            if (refreshToken == null || refreshToken.isBlank()) {
                throw new InvalidRequestException(
                        "Google did not provide offline access. Reconnect Gmail and grant access.");
            }
            GmailConnection created = new GmailConnection();
            created.setUser(users.getReferenceById(userId));
            return created;
        });

        connection.setGoogleEmail(normalizedEmail);
        connection.setEncryptedAccessToken(tokenEncryption.encrypt(accessToken, userId));
        if (refreshToken != null && !refreshToken.isBlank()) {
            connection.setEncryptedRefreshToken(tokenEncryption.encrypt(refreshToken, userId));
        }
        connection.setAccessTokenExpiresAt(accessTokenExpiresAt);
        connection.setGrantedScopes(scopes == null ? "" : scopes.trim());
        connections.save(connection);
    }

    @Transactional(readOnly = true)
    public Optional<GmailTokenSnapshot> findTokens(Long userId) {
        return connections.findByUserId(userId).map(connection -> new GmailTokenSnapshot(
                tokenEncryption.decrypt(connection.getEncryptedAccessToken(), userId),
                tokenEncryption.decrypt(connection.getEncryptedRefreshToken(), userId),
                connection.getAccessTokenExpiresAt(),
                connection.getGrantedScopes()
        ));
    }

    @Transactional
    public void updateAccessToken(
            Long userId,
            String accessToken,
            String refreshToken,
            Instant accessTokenExpiresAt,
            String scopes
    ) {
        GmailConnection connection = connections.findByUserId(userId)
                .orElseThrow(() -> new InvalidRequestException("Gmail is not connected"));
        connection.setEncryptedAccessToken(tokenEncryption.encrypt(accessToken, userId));
        if (refreshToken != null && !refreshToken.isBlank()) {
            connection.setEncryptedRefreshToken(tokenEncryption.encrypt(refreshToken, userId));
        }
        connection.setAccessTokenExpiresAt(accessTokenExpiresAt);
        if (scopes != null && !scopes.isBlank()) {
            connection.setGrantedScopes(scopes.trim());
        }
    }

    @Transactional
    public Optional<GmailTokenSnapshot> remove(Long userId) {
        Optional<GmailConnection> connection = connections.findByUserId(userId);
        if (connection.isEmpty()) {
            return Optional.empty();
        }

        GmailConnection existing = connection.get();
        GmailTokenSnapshot snapshot = new GmailTokenSnapshot(
                tokenEncryption.decrypt(existing.getEncryptedAccessToken(), userId),
                tokenEncryption.decrypt(existing.getEncryptedRefreshToken(), userId),
                existing.getAccessTokenExpiresAt(),
                existing.getGrantedScopes()
        );
        connections.delete(existing);
        return Optional.of(snapshot);
    }

    @Transactional
    public void markSynced(Long userId, Instant syncedAt) {
        GmailConnection connection = connections.findByUserId(userId)
                .orElseThrow(() -> new InvalidRequestException("Gmail is not connected"));
        connection.setLastSyncAt(syncedAt);
    }

    public record GmailTokenSnapshot(
            String accessToken,
            String refreshToken,
            Instant accessTokenExpiresAt,
            String scopes
    ) {
    }
}
