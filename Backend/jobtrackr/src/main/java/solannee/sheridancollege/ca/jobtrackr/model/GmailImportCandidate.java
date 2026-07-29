package solannee.sheridancollege.ca.jobtrackr.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "gmail_import_candidates",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_gmail_import_user_message",
                columnNames = {"user_id", "message_id_hash"}
        ),
        indexes = {
                @Index(
                        name = "idx_gmail_import_user_state_received",
                        columnList = "user_id,candidate_state,received_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class GmailImportCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "message_id_hash", nullable = false, length = 64)
    private String messageIdHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GmailImportProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DetectionConfidence confidence;

    @Column(nullable = false, length = 120)
    private String company;

    @Column(name = "job_title", nullable = false, length = 160)
    private String jobTitle;

    @Column(length = 160)
    private String location;

    @Column(name = "job_url", length = 1000)
    private String jobUrl;

    @Column(name = "application_date", nullable = false)
    private LocalDate applicationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 20)
    private EmploymentType employmentType;

    @Column(name = "source_subject", nullable = false, length = 500)
    private String sourceSubject;

    @Column(name = "source_sender", nullable = false, length = 500)
    private String sourceSender;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "candidate_state", nullable = false, length = 20)
    private GmailImportState candidateState;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imported_application_id")
    private JobApplication importedApplication;

    @Column(name = "detected_at", nullable = false, updatable = false)
    private Instant detectedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        detectedAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
