package solannee.sheridancollege.ca.jobtrackr.exception;

import org.springframework.security.core.AuthenticationException;

public class GoogleAuthenticationException extends AuthenticationException {

    public GoogleAuthenticationException(String message) {
        super(message);
    }

    public GoogleAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
