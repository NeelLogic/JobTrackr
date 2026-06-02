package solannee.sheridancollege.ca.jobtrackr.beans;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String company;

    private String role;

    private String location;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    private LocalDate dateApplied;

    private LocalDate deadline;

    private String jobLink;

    private String notes;

    private String referralName;
}