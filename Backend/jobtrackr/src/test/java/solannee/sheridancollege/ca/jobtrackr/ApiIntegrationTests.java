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
import solannee.sheridancollege.ca.jobtrackr.security.GoogleIdentityVerifier;
import solannee.sheridancollege.ca.jobtrackr.security.VerifiedGoogleIdentity;

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
