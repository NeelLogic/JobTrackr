package solannee.sheridancollege.ca.jobtrackr.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solannee.sheridancollege.ca.jobtrackr.config.GmailOAuthProperties;
import solannee.sheridancollege.ca.jobtrackr.exception.InvalidRequestException;
import solannee.sheridancollege.ca.jobtrackr.model.OAuthState;
import solannee.sheridancollege.ca.jobtrackr.model.User;
import solannee.sheridancollege.ca.jobtrackr.repository.OAuthStateRepository;
import solannee.sheridancollege.ca.jobtrackr.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class OAuthStateService {

    private static final String GMAIL_PROVIDER = "GMAIL";
    private static final int STATE_LENGTH_BYTES = 32;

    private final OAuthStateRepository states;
    private final UserRepository users;
    private final GmailOAuthProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String create(User user) {
        states.deleteByExpiresAtBefore(Instant.now());

        byte[] randomBytes = new byte[STATE_LENGTH_BYTES];
        secureRandom.nextBytes(randomBytes);
        String rawState = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        OAuthState state = new OAuthState();
        state.setUser(users.getReferenceById(user.getId()));
        state.setProvider(GMAIL_PROVIDER);
        state.setStateHash(hash(rawState));
        state.setExpiresAt(Instant.now().plus(properties.stateTtl()));
        states.save(state);
        return rawState;
    }

    @Transactional(noRollbackFor = InvalidRequestException.class)
    public OAuthStateOwner consume(String rawState) {
        if (rawState == null || rawState.isBlank()) {
            throw new InvalidRequestException("OAuth state is required");
        }

        OAuthState state = states.findForUpdate(GMAIL_PROVIDER, hash(rawState))
                .orElseThrow(() -> new InvalidRequestException(
                        "OAuth authorization request is invalid or has already been used"));
        states.delete(state);

        if (!state.getExpiresAt().isAfter(Instant.now())) {
            throw new InvalidRequestException("OAuth authorization request has expired");
        }
        return new OAuthStateOwner(state.getUser().getId(), state.getUser().getEmail());
    }

    private String hash(String rawState) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(rawState.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record OAuthStateOwner(Long userId, String email) {
    }
}
