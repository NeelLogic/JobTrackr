package solannee.sheridancollege.ca.jobtrackr.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import solannee.sheridancollege.ca.jobtrackr.dto.auth.RegisterRequest;
import solannee.sheridancollege.ca.jobtrackr.exception.ConflictException;
import solannee.sheridancollege.ca.jobtrackr.model.User;
import solannee.sheridancollege.ca.jobtrackr.repository.UserRepository;
import solannee.sheridancollege.ca.jobtrackr.security.JwtService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;
    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, passwordEncoder, authenticationManager, jwtService);
    }

    @Test
    void registrationNormalizesEmailAndHashesPassword() {
        when(passwordEncoder.encode("Password1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });
        when(jwtService.generateToken("user@example.com")).thenReturn("token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        var response = service.register(new RegisterRequest("  Test User  ", " User@Example.com ", "Password1"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("user@example.com");
        assertThat(saved.getValue().getName()).isEqualTo("Test User");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(response.user().id()).isEqualTo(42L);
        assertThat(response.token()).isEqualTo("token");
    }

    @Test
    void duplicateRegistrationFailsBeforePasswordIsEncoded() {
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(
                new RegisterRequest("User", "user@example.com", "Password1")))
                .isInstanceOf(ConflictException.class);

        verifyNoInteractions(passwordEncoder, authenticationManager, jwtService);
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginUsesTheSameNormalizedEmailForAuthenticationAndLookup() {
        User user = new User();
        user.setId(9L);
        user.setName("User");
        user.setEmail("user@example.com");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("user@example.com")).thenReturn("token");

        service.login(new solannee.sheridancollege.ca.jobtrackr.dto.auth.LoginRequest(
                " User@Example.com ", "Password1"));

        verify(authenticationManager).authenticate(argThat(authentication ->
                authentication.getName().equals("user@example.com")));
        verify(userRepository).findByEmailIgnoreCase("user@example.com");
    }
}
