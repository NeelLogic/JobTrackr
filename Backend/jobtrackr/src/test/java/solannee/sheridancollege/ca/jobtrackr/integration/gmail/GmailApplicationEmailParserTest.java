package solannee.sheridancollege.ca.jobtrackr.integration.gmail;

import org.junit.jupiter.api.Test;
import solannee.sheridancollege.ca.jobtrackr.model.ApplicationStatus;
import solannee.sheridancollege.ca.jobtrackr.model.DetectionConfidence;
import solannee.sheridancollege.ca.jobtrackr.model.EmploymentType;
import solannee.sheridancollege.ca.jobtrackr.model.GmailImportProvider;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class GmailApplicationEmailParserTest {

    private final GmailApplicationEmailParser parser = new GmailApplicationEmailParser();

    @Test
    void parsesAWorkdaySubmissionWithoutPersistingMessageContent() {
        GmailMessage message = new GmailMessage(
                "gmail-message-1",
                "Application submitted",
                "Example Corp Recruiting <notifications@myworkday.com>",
                Instant.parse("2026-07-20T14:30:00Z"),
                """
                        You successfully submitted an application for Software Developer at Example Corp.
                        Location: Toronto, ON
                        Employment type: Full-time
                        https://example.wd5.myworkdayjobs.com/jobs/job/Toronto/Software-Developer_R123
                        """
        );

        ParsedGmailApplication result = parser.parse(message).orElseThrow();

        assertThat(result.provider()).isEqualTo(GmailImportProvider.WORKDAY);
        assertThat(result.confidence()).isEqualTo(DetectionConfidence.HIGH);
        assertThat(result.company()).isEqualTo("Example Corp");
        assertThat(result.jobTitle()).isEqualTo("Software Developer");
        assertThat(result.location()).isEqualTo("Toronto, ON");
        assertThat(result.jobUrl()).contains("myworkdayjobs.com");
        assertThat(result.applicationDate()).isEqualTo("2026-07-20");
        assertThat(result.status()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(result.employmentType()).isEqualTo(EmploymentType.FULL_TIME);
    }

    @Test
    void detectsGenericInterviewAndEmploymentType() {
        GmailMessage message = new GmailMessage(
                "gmail-message-2",
                "Interview invitation: Software Engineer Intern at Acme",
                "Acme Talent <talent@acme.example>",
                Instant.parse("2026-07-21T12:00:00Z"),
                "We would like to schedule an interview for this summer internship."
        );

        ParsedGmailApplication result = parser.parse(message).orElseThrow();

        assertThat(result.provider()).isEqualTo(GmailImportProvider.GENERIC);
        assertThat(result.company()).isEqualTo("Acme");
        assertThat(result.jobTitle()).isEqualTo("Software Engineer Intern");
        assertThat(result.status()).isEqualTo(ApplicationStatus.INTERVIEW);
        assertThat(result.employmentType()).isEqualTo(EmploymentType.INTERNSHIP);
    }

    @Test
    void rejectionSignalTakesPriorityOverAnInterviewReference() {
        GmailMessage message = new GmailMessage(
                "gmail-message-3",
                "Update on your application",
                "Northstar Recruiting <careers@northstar.example>",
                Instant.parse("2026-07-22T12:00:00Z"),
                """
                        Your application to Northstar for Backend Developer.
                        Unfortunately, after interviewing candidates, we are not moving forward.
                        """
        );

        ParsedGmailApplication result = parser.parse(message).orElseThrow();

        assertThat(result.company()).isEqualTo("Northstar");
        assertThat(result.jobTitle()).isEqualTo("Backend Developer");
        assertThat(result.status()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void ignoresUnrelatedMessagesThatOnlyMentionAnApplicationConcept() {
        GmailMessage message = new GmailMessage(
                "gmail-message-4",
                "Weekly engineering newsletter",
                "Engineering News <news@engineering.example>",
                Instant.parse("2026-07-22T12:00:00Z"),
                "This week we compare web application architecture patterns."
        );

        assertThat(parser.parse(message)).isEmpty();
    }
}
