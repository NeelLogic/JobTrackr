package solannee.sheridancollege.ca.jobtrackr.dto.auth;

import solannee.sheridancollege.ca.jobtrackr.model.AuthProvider;

import java.time.Instant;

public record ConnectedIdentityResponse(AuthProvider provider, Instant connectedAt) {
}
