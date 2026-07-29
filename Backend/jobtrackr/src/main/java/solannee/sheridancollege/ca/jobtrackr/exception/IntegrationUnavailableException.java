package solannee.sheridancollege.ca.jobtrackr.exception;

public class IntegrationUnavailableException extends RuntimeException {

    public IntegrationUnavailableException(String message) {
        super(message);
    }

    public IntegrationUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
