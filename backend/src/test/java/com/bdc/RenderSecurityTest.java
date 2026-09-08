package com.bdc;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
@SpringBootTest(properties={"DEMO_PASSWORD=render-test-only-password","spring.datasource.url=jdbc:h2:mem:renderTest;MODE=MySQL","spring.jpa.hibernate.ddl-auto=create-drop"})
@ActiveProfiles("render") @AutoConfigureMockMvc
class RenderSecurityTest {
 @Autowired MockMvc mvc;
 @Test void healthPublicButPrivateDataProtected()throws Exception{
  mvc.perform(get("/api/health")).andExpect(status().isOk());
  mvc.perform(get("/api/audit")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/conversations")).andExpect(status().isUnauthorized());
  mvc.perform(get("/")).andExpect(status().is3xxRedirection());
 }
 @Test void authenticatedChatRequiresCsrf()throws Exception{
  String body="{\"question\":\"Show revenue for the last 12 months.\"}";
  mvc.perform(post("/api/chat").with(user("demo")).contentType("application/json").content(body)).andExpect(status().isForbidden());
  mvc.perform(post("/api/chat").with(user("demo")).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk());
  mvc.perform(get("/api/csrf").with(user("demo"))).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
 }
 @Test void configuredLoginWorks()throws Exception{
  mvc.perform(formLogin().user("demo").password("render-test-only-password")).andExpect(authenticated().withUsername("demo"));
  mvc.perform(formLogin().user("demo").password("wrong")).andExpect(unauthenticated());
 }
}
