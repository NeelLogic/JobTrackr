package solannee.sheridancollege.ca.jobtrackr.security;

public interface GoogleIdentityVerifier {

    VerifiedGoogleIdentity verify(String credential);
}
