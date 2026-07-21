package solannee.sheridancollege.ca.jobtrackr.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Table(name = "job_applications", indexes = {
    @Index(name = "idx_app_user_status", columnList = "user_id,status"),
    @Index(name = "idx_app_user_date", columnList = "user_id,applicationDate")
})
@Getter @Setter @NoArgsConstructor
public class JobApplication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false, length = 120) private String company;
    @Column(nullable = false, length = 160) private String jobTitle;
    @Column(length = 160) private String location;
    @Column(length = 1000) private String jobUrl;
    private LocalDate applicationDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ApplicationStatus status;
    @Enumerated(EnumType.STRING) @Column(length = 20) private EmploymentType employmentType;
    @Column(precision = 12, scale = 2) private BigDecimal salaryMin;
    @Column(precision = 12, scale = 2) private BigDecimal salaryMax;
    @Column(length = 3) private String salaryCurrency;
    @Column(length = 10000) private String notes;
    private LocalDate followUpDate;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
