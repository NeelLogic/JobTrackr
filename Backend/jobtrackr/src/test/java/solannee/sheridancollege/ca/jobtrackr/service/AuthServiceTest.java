package solannee.sheridancollege.ca.jobtrackr.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import solannee.sheridancollege.ca.jobtrackr.dto.auth.LoginRequest;
import solannee.sheridancollege.ca.jobtrackr.dto.auth.RegisterRequest;
import solannee.sheridancollege.ca.jobtrackr.exception.ConflictException;
import solannee.sheridancollege.ca.jobtrackr.exception.InvalidRequestException;
import solannee.sheridancollege.ca.jobtrackr.model.AuthProvider;
import solannee.sheridancollege.ca.jobtrackr.model.User;
import solannee.sheridancollege.ca.jobtrackr.model.UserIdentity;
import solannee.sheridancollege.ca.jobtrackr.repository.UserIdentityRepository;
import solannee.sheridancollege.ca.jobtrackr.repository.UserRepository;
import solannee.sheridancollege.ca.jobtrackr.security.GoogleIdentityVerifier;
import solannee.sheridancollege.ca.jobtrackr.security.JwtService;
import solannee.sheridancollege.ca.jobtrackr.security.VerifiedGoogleIdentity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserIdentityRepository identityRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;
    @Mock GoogleIdentityVerifier googleIdentityVerifier;
    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(
                userRepository,
                identityRepository,
                passwordEncoder,
                authenticationManager,
                jwtService,
                googleIdentityVerifier
        );
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

        service.login(new LoginRequest(" User@Example.com ", "Password1"));

        verify(authenticationManager).authenticate(argThat(authentication ->
                authentication.getName().equals("user@example.com")));
        verify(userRepository).findByEmailIgnoreCase("user@example.com");
    }

    @Test
    void googleLoginCreatesAUserAndIdentityWithoutAPassword() {
        var google = new VerifiedGoogleIdentity("google-123", "user@example.com", "Google User");
        when(googleIdentityVerifier.verify("credential")).thenReturn(google);
        when(identityRepository.findByProviderAndProviderSubject(
                AuthProvider.GOOGLE, "google-123")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(14L);
            return user;
        });
        when(identityRepository.save(any(UserIdentity.class))).thenAnswer(invocation -> {
            UserIdentity identity = invocation.getArgument(0);
            identity.setId(8L);
            return identity;
        });
        when(jwtService.generateToken("user@example.com")).thenReturn("token");

        var response = service.loginWithGoogle("credential");

        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(user.capture());
        assertThat(user.getValue().getPasswordHash()).isNull();
        assertThat(user.getValue().getEmail()).isEqualTo("user@example.com");
        verify(identityRepository).save(argThat(identity ->
                identity.getProvider() == AuthProvider.GOOGLE
                        && identity.getProviderSubject().equals("google-123")
                        && identity.getUser().getId().equals(14L)));
        assertThat(response.token()).isEqualTo("token");
    }

    @Test
    void googleLoginDoesNotAutomaticallyLinkAnExistingPasswordAccount() {
        var google = new VerifiedGoogleIdentity("google-123", "user@example.com", "User");
        when(googleIdentityVerifier.verify("credential")).thenReturn(google);
        when(identityRepository.findByProviderAndProviderSubject(
                AuthProvider.GOOGLE, "google-123")).thenReturn(Optional.empty());
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.loginWithGoogle("credential"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Sign in with your password");

        verify(userRepository, never()).save(any());
        verify(identityRepository, never()).save(any());
    }

    @Test
    void linkingGoogleRequiresTheSameVerifiedEmail() {
        User user = user(5L, "local@example.com");
        when(googleIdentityVerifier.verify("credential")).thenReturn(
                new VerifiedGoogleIdentity("google-123", "other@example.com", "Other User"));

        assertThatThrownBy(() -> service.linkGoogle(user, "credential"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("must match");

        verifyNoInteractions(identityRepository);
    }

    @Test
    void linkingAnAlreadyConnectedGoogleIdentityIsIdempotentForItsOwner() {
        User user = user(5L, "user@example.com");
        UserIdentity identity = new UserIdentity();
        identity.setUser(user);
        identity.setProvider(AuthProvider.GOOGLE);
        identity.setProviderSubject("google-123");
        when(googleIdentityVerifier.verify("credential")).thenReturn(
                new VerifiedGoogleIdentity("google-123", "user@example.com", "User"));
        when(identityRepository.findByProviderAndProviderSubject(
                AuthProvider.GOOGLE, "google-123")).thenReturn(Optional.of(identity));

        var response = service.linkGoogle(user, "credential");

        assertThat(response.provider()).isEqualTo(AuthProvider.GOOGLE);
        verify(identityRepository, never()).save(any());
    }

    private User user(long id, String email) {
        User user = new User();
        user.setId(id);
        user.setName("User");
        user.setEmail(email);
        return user;
    }
}
