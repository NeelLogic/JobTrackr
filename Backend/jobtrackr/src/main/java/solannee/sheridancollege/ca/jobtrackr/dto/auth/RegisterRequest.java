package solannee.sheridancollege.ca.jobtrackr.dto.auth;
import jakarta.validation.constraints.*;
public record RegisterRequest(@NotBlank @Size(max=100) String name, @NotBlank @Email @Size(max=254) String email,
 @NotBlank @Size(min=8,max=72) @Pattern(regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",message="must contain uppercase, lowercase, and number") String password) {}
