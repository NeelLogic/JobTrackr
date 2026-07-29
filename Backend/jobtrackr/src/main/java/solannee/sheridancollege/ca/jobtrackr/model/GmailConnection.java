package solannee.sheridancollege.ca.jobtrackr.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "gmail_connections",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_gmail_connections_user", columnNames = "user_id"),
                @UniqueConstraint(name = "uk_gmail_connections_email", columnNames = "google_email")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class GmailConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "google_email", nullable = false, length = 254)
    private String googleEmail;

    @Column(name = "encrypted_access_token", nullable = false, length = 4096)
    private String encryptedAccessToken;

    @Column(name = "encrypted_refresh_token", nullable = false, length = 4096)
    private String encryptedRefreshToken;

    @Column(name = "access_token_expires_at", nullable = false)
    private Instant accessTokenExpiresAt;

    @Column(name = "granted_scopes", nullable = false, length = 1000)
    private String grantedScopes;

    @Column(name = "connected_at", nullable = false, updatable = false)
    private Instant connectedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        connectedAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
