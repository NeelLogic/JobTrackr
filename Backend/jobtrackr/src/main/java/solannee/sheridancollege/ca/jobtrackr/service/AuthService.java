package solannee.sheridancollege.ca.jobtrackr.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solannee.sheridancollege.ca.jobtrackr.dto.auth.AuthResponse;
import solannee.sheridancollege.ca.jobtrackr.dto.auth.ConnectedIdentityResponse;
import solannee.sheridancollege.ca.jobtrackr.dto.auth.EmailRequest;
import solannee.sheridancollege.ca.jobtrackr.dto.auth.LoginRequest;
import solannee.sheridancollege.ca.jobtrackr.dto.auth.MessageResponse;
import solannee.sheridancollege.ca.jobtrackr.dto.auth.OtpVerificationRequest;
import solannee.sheridancollege.ca.jobtrackr.dto.auth.PasswordResetRequest;
import solannee.sheridancollege.ca.jobtrackr.dto.auth.RegisterRequest;
import solannee.sheridancollege.ca.jobtrackr.dto.auth.UserResponse;
import solannee.sheridancollege.ca.jobtrackr.exception.ConflictException;
import solannee.sheridancollege.ca.jobtrackr.exception.EmailNotVerifiedException;
import solannee.sheridancollege.ca.jobtrackr.exception.InvalidRequestException;
import solannee.sheridancollege.ca.jobtrackr.model.AuthProvider;
import solannee.sheridancollege.ca.jobtrackr.model.User;
import solannee.sheridancollege.ca.jobtrackr.model.UserIdentity;
import solannee.sheridancollege.ca.jobtrackr.repository.UserIdentityRepository;
import solannee.sheridancollege.ca.jobtrackr.repository.UserRepository;
import solannee.sheridancollege.ca.jobtrackr.security.GoogleIdentityVerifier;
import solannee.sheridancollege.ca.jobtrackr.security.JwtService;
import solannee.sheridancollege.ca.jobtrackr.security.VerifiedGoogleIdentity;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final UserIdentityRepository identities;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwt;
    private final GoogleIdentityVerifier googleIdentityVerifier;
    private final AuthCodeService authCodes;

    @Transactional
    public MessageResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(request.password()));
        user.setEmailVerified(false);
        user = users.save(user);
        authCodes.sendEmailVerification(user);
        return new MessageResponse("Check your email for the six-digit verification code");
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));
        User user = users.findByEmailIgnoreCase(email).orElseThrow();
        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException();
        }
        return response(user);
    }

    public AuthResponse verifyEmail(OtpVerificationRequest request) {
        return response(authCodes.verifyEmail(request.email(), request.code()));
    }

    public MessageResponse resendEmailVerification(EmailRequest request) {
        authCodes.resendEmailVerification(request.email());
        return new MessageResponse(
                "If the account still needs verification, a new code has been sent");
    }

    public MessageResponse requestPasswordReset(EmailRequest request) {
        authCodes.requestPasswordReset(request.email());
        return new MessageResponse(
                "If an eligible account exists, a password reset code has been sent");
    }

    public MessageResponse resetPassword(PasswordResetRequest request) {
        String passwordHash = encoder.encode(request.password());
        authCodes.resetPassword(request.email(), request.code(), passwordHash);
        return new MessageResponse("Your password has been reset");
    }

    @Transactional
    public AuthResponse loginWithGoogle(String credential) {
        VerifiedGoogleIdentity google = googleIdentityVerifier.verify(credential);
        return identities.findByProviderAndProviderSubject(AuthProvider.GOOGLE, google.subject())
                .map(UserIdentity::getUser)
                .map(this::response)
                .orElseGet(() -> createGoogleUser(google));
    }

    @Transactional
    public ConnectedIdentityResponse linkGoogle(User user, String credential) {
        VerifiedGoogleIdentity google = googleIdentityVerifier.verify(credential);
        if (!user.getEmail().equalsIgnoreCase(google.email())) {
            throw new InvalidRequestException(
                    "Google account email must match your JobTrackr account email");
        }

        var existingIdentity = identities.findByProviderAndProviderSubject(
                AuthProvider.GOOGLE, google.subject());
        if (existingIdentity.isPresent()) {
            if (existingIdentity.get().getUser().getId().equals(user.getId())) {
                return connectedIdentity(existingIdentity.get());
            }
            throw new ConflictException("This Google account is already linked to another user");
        }
        if (identities.existsByUserIdAndProvider(user.getId(), AuthProvider.GOOGLE)) {
            throw new ConflictException("A Google account is already linked to this user");
        }

        UserIdentity identity = new UserIdentity();
        identity.setUser(user);
        identity.setProvider(AuthProvider.GOOGLE);
        identity.setProviderSubject(google.subject());
        return connectedIdentity(identities.save(identity));
    }

    @Transactional(readOnly = true)
    public List<ConnectedIdentityResponse> connectedIdentities(User user) {
        return identities.findAllByUserIdOrderByCreatedAtAsc(user.getId()).stream()
                .map(this::connectedIdentity)
                .toList();
    }

    private AuthResponse createGoogleUser(VerifiedGoogleIdentity google) {
        if (users.existsByEmailIgnoreCase(google.email())) {
            throw new ConflictException(
                    "An account with this email already exists. Sign in with your password, "
                            + "then link Google from Settings.");
        }

        User user = new User();
        user.setName(google.name());
        user.setEmail(google.email());
        user.setEmailVerified(true);
        user = users.save(user);

        UserIdentity identity = new UserIdentity();
        identity.setUser(user);
        identity.setProvider(AuthProvider.GOOGLE);
        identity.setProviderSubject(google.subject());
        identities.save(identity);
        return response(user);
    }

    private ConnectedIdentityResponse connectedIdentity(UserIdentity identity) {
        return new ConnectedIdentityResponse(identity.getProvider(), identity.getCreatedAt());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private AuthResponse response(User user) {
        return new AuthResponse(
                jwt.generateToken(user.getEmail()),
                "Bearer",
                jwt.getExpirationSeconds(),
                new UserResponse(user.getId(), user.getName(), user.getEmail())
        );
    }
}
