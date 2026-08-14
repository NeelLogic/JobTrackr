package solannee.sheridancollege.ca.jobtrackr.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import solannee.sheridancollege.ca.jobtrackr.config.AuthMailProperties;
import solannee.sheridancollege.ca.jobtrackr.exception.IntegrationUnavailableException;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class SmtpAuthEmailSender implements AuthEmailSender {

    private final ObjectProvider<JavaMailSender> mailSenders;
    private final AuthMailProperties properties;

    @Override
    public boolean isAvailable() {
        return properties.enabled() && mailSenders.getIfAvailable() != null;
    }

    @Override
    public void sendEmailVerification(String email, String code, Duration lifetime) {
        send(
                email,
                "Verify your JobTrackr email",
                ("Your JobTrackr verification code is %s. It expires in %d minutes. "
                        + "If you did not create this account, you can ignore this email.")
                        .formatted(code, lifetime.toMinutes())
        );
    }

    @Override
    public void sendPasswordReset(String email, String code, Duration lifetime) {
        send(
                email,
                "Reset your JobTrackr password",
                ("Your JobTrackr password reset code is %s. It expires in %d minutes. "
                        + "If you did not request a password reset, you can ignore this email.")
                        .formatted(code, lifetime.toMinutes())
        );
    }

    private void send(String email, String subject, String body) {
        JavaMailSender mailSender = mailSenders.getIfAvailable();
        if (!properties.enabled() || mailSender == null) {
            throw unavailable(null);
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(email);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw unavailable(exception);
        }
    }

    private IntegrationUnavailableException unavailable(Exception cause) {
        String message = "Email delivery is temporarily unavailable";
        return cause == null
                ? new IntegrationUnavailableException(message)
                : new IntegrationUnavailableException(message, cause);
    }
}
