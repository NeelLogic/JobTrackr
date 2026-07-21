package solannee.sheridancollege.ca.jobtrackr;
import com.fasterxml.jackson.databind.*;import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.http.MediaType;import org.springframework.test.context.ActiveProfiles;import org.springframework.test.web.servlet.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") class ApiIntegrationTests{
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;private static int sequence;
 private String register(String prefix)throws Exception{String email=prefix+(++sequence)+"@example.com";String body="{\"name\":\"Test User\",\"email\":\""+email+"\",\"password\":\"Password1\"}";String response=mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();return json.readTree(response).get("token").asText();}
 private String create(String token,String company)throws Exception{String body="{\"company\":\""+company+"\",\"jobTitle\":\"Software Engineer\",\"location\":\"Toronto\",\"jobUrl\":\"https://example.com/job\",\"applicationDate\":\"2026-07-21\",\"status\":\"APPLIED\",\"employmentType\":\"FULL_TIME\",\"salaryMin\":70000,\"salaryMax\":90000,\"salaryCurrency\":\"CAD\"}";return mvc.perform(post("/api/applications").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();}
 @Test void protectedEndpointRejectsAnonymousUser()throws Exception{mvc.perform(get("/api/applications")).andExpect(status().isUnauthorized());}
 @Test void registrationValidatesPasswordAndDuplicateEmail()throws Exception{
  mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"A\",\"email\":\"bad\",\"password\":\"weak\"}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.email").exists()).andExpect(jsonPath("$.fieldErrors.password").exists());
  String email="duplicate"+(++sequence)+"@example.com";String body="{\"name\":\"User\",\"email\":\""+email+"\",\"password\":\"Password1\"}";mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
 }
 @Test void loginRejectsInvalidCredentials()throws Exception{mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"nobody@example.com\",\"password\":\"Wrong123\"}")).andExpect(status().isUnauthorized());}
 @Test void userCanCreateSearchUpdateAndDeleteApplication()throws Exception{
  String token=register("crud");JsonNode made=json.readTree(create(token,"Acme"));long id=made.get("id").asLong();
  mvc.perform(get("/api/applications").header("Authorization","Bearer "+token).param("search","acme")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].company").value("Acme"));
  String update="{\"company\":\"Acme Corp\",\"jobTitle\":\"Developer\",\"status\":\"INTERVIEW\"}";mvc.perform(put("/api/applications/"+id).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(update)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INTERVIEW"));
  mvc.perform(get("/api/dashboard").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.totalApplications").value(1)).andExpect(jsonPath("$.interviews").value(1));
  mvc.perform(delete("/api/applications/"+id).header("Authorization","Bearer "+token)).andExpect(status().isNoContent());
 }
 @Test void usersCannotReadOrModifyAnotherUsersApplication()throws Exception{
  String owner=register("owner"),attacker=register("attacker");long id=json.readTree(create(owner,"Private Co")).get("id").asLong();
  mvc.perform(get("/api/applications/"+id).header("Authorization","Bearer "+attacker)).andExpect(status().isNotFound());
  mvc.perform(delete("/api/applications/"+id).header("Authorization","Bearer "+attacker)).andExpect(status().isNotFound());
 }
 @Test void salaryRangeIsValidated()throws Exception{String token=register("salary");String body="{\"company\":\"Acme\",\"jobTitle\":\"Engineer\",\"status\":\"SAVED\",\"salaryMin\":100000,\"salaryMax\":50000}";mvc.perform(post("/api/applications").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Minimum salary cannot exceed maximum salary"));}
}
