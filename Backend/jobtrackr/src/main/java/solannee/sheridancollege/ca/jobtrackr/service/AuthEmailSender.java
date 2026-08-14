package solannee.sheridancollege.ca.jobtrackr.service;

import java.time.Duration;

public interface AuthEmailSender {
    boolean isAvailable();
    void sendEmailVerification(String email, String code, Duration lifetime);
    void sendPasswordReset(String email, String code, Duration lifetime);
}
