package solannee.sheridancollege.ca.jobtrackr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;
import solannee.sheridancollege.ca.jobtrackr.integration.gmail.GmailOAuthClient;
import solannee.sheridancollege.ca.jobtrackr.integration.gmail.GmailMailboxClient;
import solannee.sheridancollege.ca.jobtrackr.integration.gmail.GmailMessage;
import solannee.sheridancollege.ca.jobtrackr.integration.gmail.GoogleGmailProfile;
import solannee.sheridancollege.ca.jobtrackr.integration.gmail.GoogleOAuthTokens;
import solannee.sheridancollege.ca.jobtrackr.repository.GmailConnectionRepository;
import solannee.sheridancollege.ca.jobtrackr.repository.GmailImportCandidateRepository;
import solannee.sheridancollege.ca.jobtrackr.security.GoogleIdentityVerifier;
import solannee.sheridancollege.ca.jobtrackr.security.VerifiedGoogleIdentity;
import solannee.sheridancollege.ca.jobtrackr.service.AuthEmailSender;
import solannee.sheridancollege.ca.jobtrackr.service.GmailIntegrationService;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @MockitoBean
    GoogleIdentityVerifier googleIdentityVerifier;

    @MockitoBean
    GmailOAuthClient gmailOAuthClient;

    @MockitoBean
    GmailMailboxClient gmailMailboxClient;

    @MockitoBean
    AuthEmailSender authEmailSender;

    @Autowired
    GmailConnectionRepository gmailConnections;

    @Autowired
    GmailImportCandidateRepository gmailImportCandidates;

    private static int sequence;

    @BeforeEach
    void configureAuthEmail() {
        when(authEmailSender.isAvailable()).thenReturn(true);
    }

    private String register(String prefix) throws Exception {
        String email = prefix + (++sequence) + "@example.com";
        return registerEmail(email);
    }

    private String registerEmail(String email) throws Exception {
        String body = """
                {"name":"Test User","email":"%s","password":"Password1"}
                """.formatted(email);
        clearInvocations(authEmailSender);
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value(
                        "Check your email for the six-digit verification code"));
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(authEmailSender).sendEmailVerification(
                eq(email.trim().toLowerCase(Locale.ROOT)),
                code.capture(),
                any()
        );
        String response = mvc.perform(post("/api/auth/email-verification/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","code":"%s"}
                                """.formatted(email, code.getValue())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("token").asText();
    }

    private String googleLogin(String credential) throws Exception {
        String response = mvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"" + credential + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("token").asText();
    }

    private String create(String token, String company) throws Exception {
        String body = """
                {
                  "company":"%s",
                  "jobTitle":"Software Engineer",
                  "location":"Toronto",
                  "jobUrl":"https://example.com/job",
                  "applicationDate":"2026-07-21",
                  "status":"APPLIED",
                  "employmentType":"FULL_TIME",
                  "salaryMin":70000,
                  "salaryMax":90000,
                  "salaryCurrency":"CAD"
                }
                """.formatted(company);
        return mvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private String beginGmailConnection(String token) throws Exception {
        String response = mvc.perform(post("/api/integrations/gmail/connect")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationUrl").value(
                        org.hamcrest.Matchers.startsWith(
                                "https://accounts.google.com/o/oauth2/v2/auth")))
                .andReturn().getResponse().getContentAsString();
        String authorizationUrl = json.readTree(response).get("authorizationUrl").asText();
        return UriComponentsBuilder.fromUriString(authorizationUrl)
                .build()
                .getQueryParams()
                .getFirst("state");
    }

    private void connectGmail(String token, String email, String tokenPrefix) throws Exception {
        String state = beginGmailConnection(token);
        when(gmailOAuthClient.exchangeAuthorizationCode(tokenPrefix + "-code")).thenReturn(
                new GoogleOAuthTokens(
                        tokenPrefix + "-access",
                        tokenPrefix + "-refresh",
                        3600,
                        GmailIntegrationService.GMAIL_READONLY_SCOPE
                ));
        when(gmailOAuthClient.getProfile(tokenPrefix + "-access"))
                .thenReturn(new GoogleGmailProfile(email));
        mvc.perform(get("/api/integrations/gmail/callback")
                        .param("code", tokenPrefix + "-code")
                        .param("state", state))
                .andExpect(status().isFound());
    }

    private GmailMessage workdayMessage(String id, String company) {
        return new GmailMessage(
                id,
                "Application submitted to " + company,
                company + " Recruiting <notifications@myworkday.com>",
                Instant.parse("2026-07-20T14:30:00Z"),
                """
                        You successfully submitted an application for Software Developer at %s.
                        Location: Toronto, ON
                        https://%s.wd5.myworkdayjobs.com/jobs/job/Software-Developer_R123
                        """.formatted(company, company.toLowerCase().replace(" ", ""))
        );
    }

    @Test
    void protectedEndpointRejectsAnonymousUser() throws Exception {
        mvc.perform(get("/api/applications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedTokenIsRejected() throws Exception {
        mvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationValidatesPasswordAndDuplicateEmail() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\",\"email\":\"bad\",\"password\":\"weak\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());

        String email = "duplicate" + (++sequence) + "@example.com";
        String body = """
                {"name":"User","email":"%s","password":"Password1"}
                """.formatted(email);
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void loginRejectsInvalidCredentials() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\",\"password\":\"Wrong123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordAccountMustVerifyEmailBeforeSigningIn() throws Exception {
        String email = "verify" + (++sequence) + "@example.com";
        clearInvocations(authEmailSender);
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Verify User","email":"%s","password":"Password1"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").doesNotExist());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Password1"}
                                """.formatted(email)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Verify your email before signing in"));

        mvc.perform(post("/api/auth/email-verification/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","code":"999999"}
                                """.formatted(email)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The code is invalid or has expired"));

        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(authEmailSender).sendEmailVerification(eq(email), code.capture(), any());
        mvc.perform(post("/api/auth/email-verification/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","code":"%s"}
                                """.formatted(email, code.getValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Password1"}
                                """.formatted(email)))
                .andExpect(status().isOk());
    }

    @Test
    void verificationCodeLocksAfterFiveFailedAttempts() throws Exception {
        String email = "verify-limit" + (++sequence) + "@example.com";
        clearInvocations(authEmailSender);
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Verify User","email":"%s","password":"Password1"}
                                """.formatted(email)))
                .andExpect(status().isCreated());

        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(authEmailSender).sendEmailVerification(eq(email), code.capture(), any());
        for (int attempt = 0; attempt < 5; attempt++) {
            mvc.perform(post("/api/auth/email-verification/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"%s","code":"999999"}
                                    """.formatted(email)))
                    .andExpect(status().isBadRequest());
        }

        mvc.perform(post("/api/auth/email-verification/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","code":"%s"}
                                """.formatted(email, code.getValue())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The code is invalid or has expired"));
    }

    @Test
    void forgotPasswordUsesAGenericRequestAndSingleUseOtp() throws Exception {
        String email = "reset" + (++sequence) + "@example.com";
        registerEmail(email);
        clearInvocations(authEmailSender);

        mvc.perform(post("/api/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"missing@example.com\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value(
                        "If an eligible account exists, a password reset code has been sent"));
        verify(authEmailSender, times(0)).sendPasswordReset(anyString(), anyString(), any());

        mvc.perform(post("/api/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isAccepted());
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(authEmailSender).sendPasswordReset(eq(email), code.capture(), any());

        mvc.perform(post("/api/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","code":"%s","password":"NewPassword2"}
                                """.formatted(email, code.getValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Your password has been reset"));

        mvc.perform(post("/api/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","code":"%s","password":"AnotherPassword3"}
                                """.formatted(email, code.getValue())))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Password1"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"NewPassword2"}
                                """.formatted(email)))
                .andExpect(status().isOk());
    }

    @Test
    void googleConfigIsPublic() throws Exception {
        mvc.perform(get("/api/auth/google/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.clientId")
                        .value("jobtrackr-test-client.apps.googleusercontent.com"));
    }

    @Test
    void googleLoginCreatesAnAuthenticatedUserWithoutAPassword() throws Exception {
        String email = "google" + (++sequence) + "@example.com";
        when(googleIdentityVerifier.verify("new-google-user")).thenReturn(
                new VerifiedGoogleIdentity("subject-" + sequence, email, "Google User"));

        String token = googleLogin("new-google-user");

        mvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Password1"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void existingPasswordAccountMustSignInBeforeLinkingGoogle() throws Exception {
        String email = "link" + (++sequence) + "@example.com";
        String token = registerEmail(email);
        when(googleIdentityVerifier.verify("link-google")).thenReturn(
                new VerifiedGoogleIdentity("link-subject-" + sequence, email, "Linked User"));

        mvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"link-google\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "An account with this email already exists. Sign in with your password, "
                                + "then link Google from Settings."));

        mvc.perform(post("/api/auth/google/link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"link-google\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("GOOGLE"));

        String googleToken = googleLogin("link-google");
        mvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer " + googleToken))
                .andExpect(status().isOk());
    }

    @Test
    void googleLinkRequiresAuthenticationBeforeCredentialVerification() throws Exception {
        mvc.perform(post("/api/auth/google/link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"credential\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(googleIdentityVerifier);
    }

    @Test
    void googleLinkRejectsAMismatchedAccountEmail() throws Exception {
        String token = register("mismatch");
        when(googleIdentityVerifier.verify("wrong-google")).thenReturn(
                new VerifiedGoogleIdentity("wrong-subject", "other@example.com", "Other User"));

        mvc.perform(post("/api/auth/google/link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"wrong-google\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Google account email must match your JobTrackr account email"));
    }

    @Test
    void gmailConnectionEndpointsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/integrations/gmail"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/integrations/gmail/connect"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/integrations/gmail"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/integrations/gmail/scan"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/integrations/gmail/candidates"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/integrations/gmail/candidates/1/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/integrations/gmail/candidates/1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(gmailOAuthClient);
        verifyNoInteractions(gmailMailboxClient);
    }

    @Test
    void gmailAuthorizationUsesRequiredSecurityParameters() throws Exception {
        String email = "gmail-url" + (++sequence) + "@example.com";
        String token = registerEmail(email);

        String response = mvc.perform(post("/api/integrations/gmail/connect")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String authorizationUrl = json.readTree(response).get("authorizationUrl").asText();
        var query = UriComponentsBuilder.fromUriString(authorizationUrl)
                .build()
                .getQueryParams();

        assertThat(query.getFirst("client_id"))
                .isEqualTo("jobtrackr-gmail-test-client.apps.googleusercontent.com");
        assertThat(query.getFirst("redirect_uri"))
                .isEqualTo("http://localhost:8080/api/integrations/gmail/callback");
        assertThat(query.getFirst("scope")).isEqualTo(
                GmailIntegrationService.GMAIL_READONLY_SCOPE);
        assertThat(query.getFirst("access_type")).isEqualTo("offline");
        assertThat(query.getFirst("prompt")).isEqualTo("consent");
        assertThat(query.getFirst("login_hint")).isEqualTo(email);
        assertThat(query.getFirst("state")).hasSizeGreaterThan(32);
    }

    @Test
    void callbackConnectsOnlyTheOwningUserAndStoresEncryptedTokens() throws Exception {
        String email = "gmail-owner" + (++sequence) + "@example.com";
        String ownerToken = registerEmail(email);
        String otherToken = register("gmail-other");
        String state = beginGmailConnection(ownerToken);
        GoogleOAuthTokens tokens = new GoogleOAuthTokens(
                "plain-access-token",
                "plain-refresh-token",
                3600,
                GmailIntegrationService.GMAIL_READONLY_SCOPE
        );
        when(gmailOAuthClient.exchangeAuthorizationCode("authorization-code"))
                .thenReturn(tokens);
        when(gmailOAuthClient.getProfile("plain-access-token"))
                .thenReturn(new GoogleGmailProfile(email));

        mvc.perform(get("/api/integrations/gmail/callback")
                        .param("code", "authorization-code")
                        .param("state", state))
                .andExpect(status().isFound())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .redirectedUrl("http://localhost:4200/settings?gmail=connected"));

        mvc.perform(get("/api/integrations/gmail")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.email").value(email));
        mvc.perform(get("/api/integrations/gmail")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(false))
                .andExpect(jsonPath("$.email").doesNotExist());

        var stored = gmailConnections.findAll().stream()
                .filter(connection -> connection.getGoogleEmail().equals(email))
                .findFirst()
                .orElseThrow();
        assertThat(stored.getEncryptedAccessToken()).doesNotContain("plain-access-token");
        assertThat(stored.getEncryptedRefreshToken()).doesNotContain("plain-refresh-token");

        mvc.perform(get("/api/integrations/gmail/callback")
                        .param("code", "authorization-code")
                        .param("state", state))
                .andExpect(status().isFound())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .redirectedUrl("http://localhost:4200/settings?gmail=error"));
        verify(gmailOAuthClient, times(1)).exchangeAuthorizationCode("authorization-code");
    }

    @Test
    void callbackRejectsMismatchedGmailAndConsumesState() throws Exception {
        String token = register("gmail-mismatch");
        String state = beginGmailConnection(token);
        GoogleOAuthTokens tokens = new GoogleOAuthTokens(
                "mismatch-access",
                "mismatch-refresh",
                3600,
                GmailIntegrationService.GMAIL_READONLY_SCOPE
        );
        when(gmailOAuthClient.exchangeAuthorizationCode("mismatch-code")).thenReturn(tokens);
        when(gmailOAuthClient.getProfile("mismatch-access"))
                .thenReturn(new GoogleGmailProfile("someone-else@example.com"));

        mvc.perform(get("/api/integrations/gmail/callback")
                        .param("code", "mismatch-code")
                        .param("state", state))
                .andExpect(status().isFound())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .redirectedUrl("http://localhost:4200/settings?gmail=error"));
        mvc.perform(get("/api/integrations/gmail")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(false));
        verify(gmailOAuthClient).revoke("mismatch-refresh");

        mvc.perform(get("/api/integrations/gmail/callback")
                        .param("code", "mismatch-code")
                        .param("state", state))
                .andExpect(status().isFound())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .redirectedUrl("http://localhost:4200/settings?gmail=error"));
        verify(gmailOAuthClient, times(1)).exchangeAuthorizationCode("mismatch-code");
    }

    @Test
    void userCanDisconnectTheirOwnGmailConnection() throws Exception {
        String email = "gmail-disconnect" + (++sequence) + "@example.com";
        String token = registerEmail(email);
        String state = beginGmailConnection(token);
        when(gmailOAuthClient.exchangeAuthorizationCode("disconnect-code")).thenReturn(
                new GoogleOAuthTokens(
                        "disconnect-access",
                        "disconnect-refresh",
                        3600,
                        null
                ));
        when(gmailOAuthClient.getProfile("disconnect-access"))
                .thenReturn(new GoogleGmailProfile(email));
        mvc.perform(get("/api/integrations/gmail/callback")
                        .param("code", "disconnect-code")
                        .param("state", state))
                .andExpect(status().isFound());

        mvc.perform(delete("/api/integrations/gmail")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/integrations/gmail")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(false));
        verify(gmailOAuthClient).revoke("disconnect-refresh");
    }

    @Test
    void gmailScanDetectsCandidatesUpdatesSyncAndDeduplicatesMessages() throws Exception {
        String email = "gmail-scan" + (++sequence) + "@example.com";
        String token = registerEmail(email);
        String tokenPrefix = "scan-" + sequence;
        connectGmail(token, email, tokenPrefix);
        GmailMessage application = workdayMessage("private-gmail-message-" + sequence, "Maple Labs");
        GmailMessage unrelated = new GmailMessage(
                "newsletter-" + sequence,
                "Weekly engineering newsletter",
                "News <news@example.com>",
                Instant.parse("2026-07-21T12:00:00Z"),
                "A web application architecture article"
        );
        when(gmailMailboxClient.listMessages(
                eq(tokenPrefix + "-access"), anyString(), eq(100)))
                .thenReturn(List.of(application, unrelated));

        String firstScan = mvc.perform(post("/api/integrations/gmail/scan")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messagesScanned").value(2))
                .andExpect(jsonPath("$.matchesDetected").value(1))
                .andExpect(jsonPath("$.candidatesAdded").value(1))
                .andExpect(jsonPath("$.duplicatesSkipped").value(0))
                .andExpect(jsonPath("$.candidates.length()").value(1))
                .andExpect(jsonPath("$.candidates[0].provider").value("WORKDAY"))
                .andExpect(jsonPath("$.candidates[0].company").value("Maple Labs"))
                .andExpect(jsonPath("$.candidates[0].jobTitle").value("Software Developer"))
                .andReturn().getResponse().getContentAsString();

        long candidateId = json.readTree(firstScan).get("candidates").get(0).get("id").asLong();
        var stored = gmailImportCandidates.findById(candidateId).orElseThrow();
        assertThat(stored.getMessageIdHash())
                .hasSize(64)
                .doesNotContain(application.id());
        assertThat(gmailConnections.findAll().stream()
                .filter(connection -> connection.getGoogleEmail().equals(email))
                .findFirst().orElseThrow().getLastSyncAt()).isNotNull();

        mvc.perform(post("/api/integrations/gmail/scan")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidatesAdded").value(0))
                .andExpect(jsonPath("$.duplicatesSkipped").value(1))
                .andExpect(jsonPath("$.candidates.length()").value(1));
    }

    @Test
    void userReviewsAndExplicitlyImportsAGmailCandidateOnlyOnce() throws Exception {
        String email = "gmail-import" + (++sequence) + "@example.com";
        String token = registerEmail(email);
        String tokenPrefix = "import-" + sequence;
        connectGmail(token, email, tokenPrefix);
        when(gmailMailboxClient.listMessages(
                eq(tokenPrefix + "-access"), anyString(), eq(100)))
                .thenReturn(List.of(workdayMessage("import-message-" + sequence, "Northstar")));

        String scan = mvc.perform(post("/api/integrations/gmail/scan")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long candidateId = json.readTree(scan).get("candidates").get(0).get("id").asLong();

        mvc.perform(post("/api/integrations/gmail/candidates/" + candidateId + "/import")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "company":"",
                                  "jobTitle":"Software Developer",
                                  "applicationDate":"2026-07-20",
                                  "status":"APPLIED",
                                  "employmentType":"FULL_TIME"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.company").exists());

        String reviewed = """
                {
                  "company":"Northstar Technologies",
                  "jobTitle":"Junior Software Developer",
                  "location":"Toronto, ON",
                  "jobUrl":"https://northstar.example/jobs/123",
                  "applicationDate":"2026-07-20",
                  "status":"APPLIED",
                  "employmentType":"FULL_TIME",
                  "notes":"Imported after reviewing the Gmail suggestion."
                }
                """;
        mvc.perform(post("/api/integrations/gmail/candidates/" + candidateId + "/import")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewed))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.company").value("Northstar Technologies"))
                .andExpect(jsonPath("$.jobTitle").value("Junior Software Developer"));

        mvc.perform(get("/api/integrations/gmail/candidates")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].company")
                        .value("Northstar Technologies"));
        mvc.perform(post("/api/integrations/gmail/candidates/" + candidateId + "/import")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewed))
                .andExpect(status().isConflict());
    }

    @Test
    void usersCannotSeeImportOrDismissAnotherUsersGmailCandidates() throws Exception {
        String email = "gmail-private" + (++sequence) + "@example.com";
        String owner = registerEmail(email);
        String attacker = register("gmail-candidate-attacker");
        String tokenPrefix = "private-" + sequence;
        connectGmail(owner, email, tokenPrefix);
        when(gmailMailboxClient.listMessages(
                eq(tokenPrefix + "-access"), anyString(), eq(100)))
                .thenReturn(List.of(workdayMessage("private-message-" + sequence, "Private Co")));

        String scan = mvc.perform(post("/api/integrations/gmail/scan")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long candidateId = json.readTree(scan).get("candidates").get(0).get("id").asLong();

        mvc.perform(get("/api/integrations/gmail/candidates")
                        .header("Authorization", "Bearer " + attacker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post("/api/integrations/gmail/candidates/" + candidateId + "/import")
                        .header("Authorization", "Bearer " + attacker)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "company":"Stolen",
                                  "jobTitle":"Developer",
                                  "applicationDate":"2026-07-20",
                                  "status":"APPLIED",
                                  "employmentType":"FULL_TIME"
                                }
                                """))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/integrations/gmail/candidates/" + candidateId)
                        .header("Authorization", "Bearer " + attacker))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/api/integrations/gmail/candidates/" + candidateId)
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/integrations/gmail/candidates/" + candidateId)
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isConflict());
    }

    @Test
    void userCanCreateSearchUpdateAndDeleteApplication() throws Exception {
        String token = register("crud");
        JsonNode made = json.readTree(create(token, "Acme"));
        long id = made.get("id").asLong();

        mvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer " + token)
                        .param("search", "acme"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].company").value("Acme"));

        String update = """
                {
                  "company":"Acme Corp",
                  "jobTitle":"Developer",
                  "applicationDate":"2026-07-21",
                  "status":"INTERVIEW",
                  "employmentType":"FULL_TIME"
                }
                """;
        mvc.perform(put("/api/applications/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INTERVIEW"));

        mvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalApplications").value(1))
                .andExpect(jsonPath("$.interviews").value(1));

        mvc.perform(delete("/api/applications/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void usersCannotReadOrModifyAnotherUsersApplication() throws Exception {
        String owner = register("owner");
        String attacker = register("attacker");
        long id = json.readTree(create(owner, "Private Co")).get("id").asLong();

        mvc.perform(get("/api/applications/" + id)
                        .header("Authorization", "Bearer " + attacker))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/applications/" + id)
                        .header("Authorization", "Bearer " + attacker))
                .andExpect(status().isNotFound());
    }

    @Test
    void analyticsCompaniesAndFollowUpsAreUserScoped() throws Exception {
        String owner = register("insights-owner");
        String other = register("insights-other");
        long ownerApplication = json.readTree(create(owner, "Private Analytics Co"))
                .get("id").asLong();
        create(other, "Other User Co");

        String update = """
                {
                  "company":"Private Analytics Co",
                  "jobTitle":"Software Engineer",
                  "applicationDate":"2026-07-21",
                  "status":"INTERVIEW",
                  "employmentType":"FULL_TIME",
                  "followUpDate":"2026-07-22"
                }
                """;
        mvc.perform(put("/api/applications/" + ownerApplication)
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isOk());

        mvc.perform(get("/api/analytics")
                        .header("Authorization", "Bearer " + owner)
                        .param("range", "ALL_TIME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationsInRange").value(1))
                .andExpect(jsonPath("$.funnel[2].stage").value("INTERVIEW"))
                .andExpect(jsonPath("$.funnel[2].applications").value(1))
                .andExpect(jsonPath("$.topCompanies[0].company").value("Private Analytics Co"));

        mvc.perform(get("/api/companies")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCompanies").value(1))
                .andExpect(jsonPath("$.companies[0].company").value("Private Analytics Co"));

        mvc.perform(get("/api/follow-ups")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdueCount").value(1))
                .andExpect(jsonPath("$.overdue[0].company").value("Private Analytics Co"));
    }

    @Test
    void insightsRejectInvalidParametersAndAnonymousRequests() throws Exception {
        String token = register("insights-validation");

        mvc.perform(get("/api/analytics")
                        .header("Authorization", "Bearer " + token)
                        .param("range", "LAST_CENTURY"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Range must be THIRTY_DAYS, NINETY_DAYS, SIX_MONTHS, or ALL_TIME"
                ));
        mvc.perform(get("/api/companies")
                        .header("Authorization", "Bearer " + token)
                        .param("sort", "user.passwordHash"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Unsupported company sort field: user.passwordHash"
                ));
        mvc.perform(get("/api/follow-ups"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void salaryRangeIsValidated() throws Exception {
        String token = register("salary");
        String body = """
                {
                  "company":"Acme",
                  "jobTitle":"Engineer",
                  "status":"SAVED",
                  "employmentType":"FULL_TIME",
                  "salaryMin":100000,
                  "salaryMax":50000
                }
                """;
        mvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Minimum salary cannot exceed maximum salary"));
    }

    @Test
    void followUpDateRequiresFourDigitYear() throws Exception {
        String token = register("followup-year");
        String body = """
                {
                  "company":"Acme",
                  "jobTitle":"Engineer",
                  "status":"SAVED",
                  "employmentType":"FULL_TIME",
                  "followUpDate":"+123456-07-22"
                }
                """;

        mvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.followUpDate")
                        .value("must use a four-digit year"));
    }

    @Test
    void invalidQueryParametersReturnValidationError() throws Exception {
        String token = register("query");
        mvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer " + token)
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
        mvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer " + token)
                        .param("sort", "user.passwordHash"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Unsupported sort field: user.passwordHash"));
    }

    @Test
    void listFiltersByStatusAndPaginates() throws Exception {
        String token = register("filter");
        create(token, "First");
        create(token, "Second");
        String saved = """
                {
                  "company":"Draft",
                  "jobTitle":"Developer",
                  "status":"SAVED",
                  "employmentType":"FULL_TIME"
                }
                """;
        mvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saved))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "APPLIED")
                        .param("size", "1")
                        .param("page", "1")
                        .param("sort", "company")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].company").value("Second"));
    }
}
