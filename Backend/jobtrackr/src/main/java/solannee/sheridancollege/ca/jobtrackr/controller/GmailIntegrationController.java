package solannee.sheridancollege.ca.jobtrackr.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import solannee.sheridancollege.ca.jobtrackr.dto.application.ApplicationRequest;
import solannee.sheridancollege.ca.jobtrackr.dto.application.ApplicationResponse;
import solannee.sheridancollege.ca.jobtrackr.dto.integration.GmailAuthorizationResponse;
import solannee.sheridancollege.ca.jobtrackr.dto.integration.GmailConnectionResponse;
import solannee.sheridancollege.ca.jobtrackr.dto.integration.GmailImportCandidateResponse;
import solannee.sheridancollege.ca.jobtrackr.dto.integration.GmailImportScanResponse;
import solannee.sheridancollege.ca.jobtrackr.service.CurrentUserService;
import solannee.sheridancollege.ca.jobtrackr.service.GmailImportService;
import solannee.sheridancollege.ca.jobtrackr.service.GmailIntegrationService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/integrations/gmail")
@RequiredArgsConstructor
@Validated
public class GmailIntegrationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GmailIntegrationController.class);

    private final GmailIntegrationService gmail;
    private final GmailImportService gmailImport;
    private final CurrentUserService currentUserService;

    @GetMapping
    GmailConnectionResponse status(Authentication authentication) {
        return gmail.status(currentUserService.require(authentication));
    }

    @PostMapping("/connect")
    GmailAuthorizationResponse connect(Authentication authentication) {
        return gmail.beginAuthorization(currentUserService.require(authentication));
    }

    @GetMapping("/callback")
    ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error
    ) {
        String result = "connected";
        try {
            if (error != null && !error.isBlank()) {
                gmail.cancelAuthorization(state);
                result = "denied";
            } else {
                gmail.completeAuthorization(code, state);
            }
        } catch (RuntimeException exception) {
            LOGGER.warn("Gmail OAuth callback failed: {}", exception.getClass().getSimpleName());
            result = "error";
        }
        URI location = gmail.frontendResultUri(result);
        return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
    }

    @DeleteMapping
    ResponseEntity<Void> disconnect(Authentication authentication) {
        gmail.disconnect(currentUserService.require(authentication));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/scan")
    GmailImportScanResponse scan(Authentication authentication) {
        return gmailImport.scan(currentUserService.require(authentication));
    }

    @GetMapping("/candidates")
    List<GmailImportCandidateResponse> candidates(Authentication authentication) {
        return gmailImport.pending(currentUserService.require(authentication));
    }

    @PostMapping("/candidates/{id}/import")
    ResponseEntity<ApplicationResponse> importCandidate(
            Authentication authentication,
            @PathVariable @Positive Long id,
            @Valid @RequestBody ApplicationRequest request
    ) {
        ApplicationResponse imported = gmailImport.importCandidate(
                currentUserService.require(authentication), id, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/applications/{id}")
                .buildAndExpand(imported.id())
                .toUri();
        return ResponseEntity.created(location).body(imported);
    }

    @DeleteMapping("/candidates/{id}")
    ResponseEntity<Void> dismissCandidate(
            Authentication authentication,
            @PathVariable @Positive Long id
    ) {
        gmailImport.dismiss(currentUserService.require(authentication), id);
        return ResponseEntity.noContent().build();
    }
}
