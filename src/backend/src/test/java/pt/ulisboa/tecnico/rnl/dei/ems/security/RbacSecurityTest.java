package pt.ulisboa.tecnico.rnl.dei.ems.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class RbacSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private String adminToken;
	private String staffToken;
	private String teacherToken;
	private String studentToken;

	@BeforeEach
	void loginSeedUsers() throws Exception {
		adminToken = login("admin@dei.tecnico.ulisboa.pt", "admin123");
		staffToken = login("staff@dei.tecnico.ulisboa.pt", "staff123");
		teacherToken = login("teacher@dei.tecnico.ulisboa.pt", "teacher123");
		studentToken = login("student@dei.tecnico.ulisboa.pt", "student123");
	}

	private String login(String email, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
		return body.get("token").asText();
	}

	@Test
	void studentCannotListPeople() throws Exception {
		mockMvc.perform(get("/people").header("Authorization", bearer(studentToken)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value(2005));
	}

	@Test
	void studentCanListOwnExams() throws Exception {
		mockMvc.perform(get("/exams/mine").header("Authorization", bearer(studentToken)))
				.andExpect(status().isOk());
	}

	@Test
	void teacherCanAccessGradingInbox() throws Exception {
		mockMvc.perform(get("/evaluates/mine").header("Authorization", bearer(teacherToken)))
				.andExpect(status().isOk());
	}

	@Test
	void teacherCannotLockExams() throws Exception {
		mockMvc.perform(post("/exams/lock").header("Authorization", bearer(teacherToken)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value(2005));
	}

	@Test
	void staffCannotListSchools() throws Exception {
		mockMvc.perform(get("/schools").header("Authorization", bearer(staffToken)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value(2005));
	}

	@Test
	void staffOnlySeesPeopleFromOwnSchool() throws Exception {
		MvcResult result = mockMvc.perform(get("/people").header("Authorization", bearer(staffToken)))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode people = objectMapper.readTree(result.getResponse().getContentAsString());
		List<String> emails = new ArrayList<>();
		people.forEach(p -> emails.add(p.get("email").asText()));

		assertThat(emails).contains("student@dei.tecnico.ulisboa.pt");
		assertThat(emails).doesNotContain("aluno6@dei.tecnico.ulisboa.pt");
		assertThat(emails).doesNotContain("admin@dei.tecnico.ulisboa.pt");
	}

	@Test
	void teacherOnlySeesPeopleFromOwnSchool() throws Exception {
		MvcResult result = mockMvc.perform(get("/people").header("Authorization", bearer(teacherToken)))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode people = objectMapper.readTree(result.getResponse().getContentAsString());
		List<String> emails = new ArrayList<>();
		people.forEach(p -> emails.add(p.get("email").asText()));

		assertThat(emails).contains("aluno6@dei.tecnico.ulisboa.pt");
		assertThat(emails).doesNotContain("student@dei.tecnico.ulisboa.pt");
		assertThat(emails).doesNotContain("admin@dei.tecnico.ulisboa.pt");
	}

	@Test
	void staffCannotCreateAdministrator() throws Exception {
		mockMvc.perform(post("/people")
				.header("Authorization", bearer(staffToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "Intruder Admin",
						  "email": "intruder-admin@dei.tecnico.ulisboa.pt",
						  "password": "admin123",
						  "type": "ADMINISTRATOR"
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(2005));
	}

	@Test
	void staffCanCreateStudentInOwnSchool() throws Exception {
		mockMvc.perform(post("/people")
				.header("Authorization", bearer(staffToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "Novo Aluno ESLFB",
						  "email": "rbac-new-student@dei.tecnico.ulisboa.pt",
						  "password": "student123",
						  "type": "STUDENT",
						  "schoolCode": "ESLFB"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.type").value("STUDENT"))
				.andExpect(jsonPath("$.schoolCode").value("ESLFB"));
	}

	@Test
	void staffCannotCreateStudentInOtherSchool() throws Exception {
		mockMvc.perform(post("/people")
				.header("Authorization", bearer(staffToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "Aluno ESRDL",
						  "email": "rbac-wrong-school@dei.tecnico.ulisboa.pt",
						  "password": "student123",
						  "type": "STUDENT",
						  "schoolCode": "ESRDL"
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(2005));
	}

	@Test
	void adminCanLockExams() throws Exception {
		mockMvc.perform(post("/exams/lock").header("Authorization", bearer(adminToken)))
				.andExpect(status().isOk());
	}

	@Test
	void teacherCannotGradeSomeoneElsesEvaluate() throws Exception {
		MvcResult inbox = mockMvc.perform(get("/evaluates/mine").header("Authorization", bearer(teacherToken)))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode items = objectMapper.readTree(inbox.getResponse().getContentAsString());
		if (items.isEmpty()) {
			return;
		}
		long evaluateId = items.get(0).get("id").asLong();

		mockMvc.perform(put("/evaluates/" + evaluateId + "/grade")
				.header("Authorization", bearer(login("carlos.mendes@dei.tecnico.ulisboa.pt", "teacher123")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"grade\":1.0}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(2005));
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}
}
