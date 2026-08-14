package solannee.sheridancollege.ca.jobtrackr.exception;

public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException() {
        super("Verify your email before signing in");
    }
}
