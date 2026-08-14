package solannee.sheridancollege.ca.jobtrackr.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import solannee.sheridancollege.ca.jobtrackr.config.AuthMailProperties;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmtpAuthEmailSenderTest {

    @Mock ObjectProvider<JavaMailSender> mailSenders;
    @Mock JavaMailSender mailSender;

    private SmtpAuthEmailSender sender;

    @BeforeEach
    void setUp() {
        sender = new SmtpAuthEmailSender(
                mailSenders,
                new AuthMailProperties("smtp.example.com", "no-reply@jobtrackr.app")
        );
        when(mailSenders.getIfAvailable()).thenReturn(mailSender);
    }

    @Test
    void verificationEmailIncludesTheCodeAndExpiry() {
        sender.sendEmailVerification("user@example.com", "123456", Duration.ofMinutes(10));

        SimpleMailMessage message = captureMessage();
        assertThat(message.getTo()).containsExactly("user@example.com");
        assertThat(message.getFrom()).isEqualTo("no-reply@jobtrackr.app");
        assertThat(message.getSubject()).isEqualTo("Verify your JobTrackr email");
        assertThat(message.getText())
                .contains("123456")
                .contains("10 minutes")
                .doesNotContain("%s", "%d");
    }

    @Test
    void passwordResetEmailIncludesTheCodeAndExpiry() {
        sender.sendPasswordReset("user@example.com", "654321", Duration.ofMinutes(10));

        SimpleMailMessage message = captureMessage();
        assertThat(message.getSubject()).isEqualTo("Reset your JobTrackr password");
        assertThat(message.getText())
                .contains("654321")
                .contains("10 minutes")
                .doesNotContain("%s", "%d");
    }

    private SimpleMailMessage captureMessage() {
        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(message.capture());
        return message.getValue();
    }
}
