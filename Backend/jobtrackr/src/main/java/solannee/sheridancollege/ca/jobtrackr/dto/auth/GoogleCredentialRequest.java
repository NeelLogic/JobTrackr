package solannee.sheridancollege.ca.jobtrackr.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record GoogleCredentialRequest(
        @NotBlank(message = "Google credential is required")
        String credential
) {
}
