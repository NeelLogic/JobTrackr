package solannee.sheridancollege.ca.jobtrackr.security;

public record VerifiedGoogleIdentity(String subject, String email, String name) {
}
