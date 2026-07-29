package solannee.sheridancollege.ca.jobtrackr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;
import solannee.sheridancollege.ca.jobtrackr.integration.gmail.GmailOAuthClient;
import solannee.sheridancollege.ca.jobtrackr.integration.gmail.GoogleGmailProfile;
import solannee.sheridancollege.ca.jobtrackr.integration.gmail.GoogleOAuthTokens;
import solannee.sheridancollege.ca.jobtrackr.repository.GmailConnectionRepository;
import solannee.sheridancollege.ca.jobtrackr.security.GoogleIdentityVerifier;
import solannee.sheridancollege.ca.jobtrackr.security.VerifiedGoogleIdentity;
import solannee.sheridancollege.ca.jobtrackr.service.GmailIntegrationService;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Autowired
    GmailConnectionRepository gmailConnections;

    private static int sequence;

    private String register(String prefix) throws Exception {
        String email = prefix + (++sequence) + "@example.com";
        return registerEmail(email);
    }

    private String registerEmail(String email) throws Exception {
        String body = """
                {"name":"Test User","email":"%s","password":"Password1"}
                """.formatted(email);
        String response = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
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

        verifyNoInteractions(gmailOAuthClient);
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
                        GmailIntegrationService.GMAIL_READONLY_SCOPE
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
