package solannee.sheridancollege.ca.jobtrackr.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import solannee.sheridancollege.ca.jobtrackr.config.GoogleAuthProperties;
import solannee.sheridancollege.ca.jobtrackr.dto.auth.AuthResponse;
import solannee.sheridancollege.ca.jobtrackr.dto.auth.ConnectedIdentityResponse;
import solannee.sheridancollege.ca.jobtrackr.dto.auth.EmailRequest;
import solannee.sheridancollege.ca.jobtrackr.dto.auth.GoogleAuthConfigResponse;
import solannee.sheridancollege.ca.jobtrackr.dto.auth.GoogleCredentialRequest;
import solannee.sheridancollege.ca.jobtrackr.dto.auth.LoginRequest;
import solannee.sheridancollege.ca.jobtrackr.dto.auth.MessageResponse;
import solannee.sheridancollege.ca.jobtrackr.dto.auth.OtpVerificationRequest;
import solannee.sheridancollege.ca.jobtrackr.dto.auth.PasswordResetRequest;
import solannee.sheridancollege.ca.jobtrackr.dto.auth.RegisterRequest;
import solannee.sheridancollege.ca.jobtrackr.service.AuthService;
import solannee.sheridancollege.ca.jobtrackr.service.CurrentUserService;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;
    private final CurrentUserService currentUserService;
    private final GoogleAuthProperties googleProperties;

    @PostMapping("/register")
    ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(request));
    }

    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @PostMapping("/email-verification/confirm")
    AuthResponse verifyEmail(@Valid @RequestBody OtpVerificationRequest request) {
        return service.verifyEmail(request);
    }

    @PostMapping("/email-verification/resend")
    ResponseEntity<MessageResponse> resendEmailVerification(
            @Valid @RequestBody EmailRequest request
    ) {
        return ResponseEntity.accepted().body(service.resendEmailVerification(request));
    }

    @PostMapping("/password-reset/request")
    ResponseEntity<MessageResponse> requestPasswordReset(@Valid @RequestBody EmailRequest request) {
        return ResponseEntity.accepted().body(service.requestPasswordReset(request));
    }

    @PostMapping("/password-reset/confirm")
    MessageResponse resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        return service.resetPassword(request);
    }

    @GetMapping("/google/config")
    GoogleAuthConfigResponse googleConfig() {
        return new GoogleAuthConfigResponse(
                googleProperties.enabled(),
                googleProperties.enabled() ? googleProperties.clientId() : null
        );
    }

    @PostMapping("/google")
    AuthResponse googleLogin(@Valid @RequestBody GoogleCredentialRequest request) {
        return service.loginWithGoogle(request.credential());
    }

    @PostMapping("/google/link")
    ConnectedIdentityResponse linkGoogle(
            Authentication authentication,
            @Valid @RequestBody GoogleCredentialRequest request
    ) {
        return service.linkGoogle(currentUserService.require(authentication), request.credential());
    }

    @GetMapping("/identities")
    List<ConnectedIdentityResponse> connectedIdentities(Authentication authentication) {
        return service.connectedIdentities(currentUserService.require(authentication));
    }
}
