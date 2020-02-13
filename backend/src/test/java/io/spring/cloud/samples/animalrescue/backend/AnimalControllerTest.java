package io.spring.cloud.samples.animalrescue.backend;

import java.util.HashMap;
import java.util.Map;

import mockit.Mock;
import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.test.AbstractCfEnvTests;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import static org.hamcrest.Matchers.hasItem;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class AnimalControllerTest {

	@Autowired
	private WebTestClient webTestClient;

	@MockBean(answer = Answers.RETURNS_DEEP_STUBS)
	private CfEnv cfEnv;

	@Test
	void getAllAnimals() {
		webTestClient
			.get()
			.uri("/animals")
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$.length()").isEqualTo(10)
			.jsonPath("$[0].id").isEqualTo(1)
			.jsonPath("$[0].name").isEqualTo("Chocobo")
			.jsonPath("$[0].avatarUrl").isNotEmpty()
			.jsonPath("$[0].description").isNotEmpty()
			.jsonPath("$[0].rescueDate").isNotEmpty()
			.jsonPath("$[0].adoptionRequests.length()").isEqualTo(3)
			.jsonPath("$[0].adoptionRequests[0].adopterName").isNotEmpty()
			.jsonPath("$[0].adoptionRequests[0].email").isNotEmpty()
			.jsonPath("$[0].adoptionRequests[0].notes").isNotEmpty();
	}

	@Test
	@WithMockUser(username = "test-user", authorities = { "adoption.request" })
	void submitAdoptionRequest() {
		String testEmail = "a@email.com";
		String testNotes = "Yaaas!";

		Map<String, String> requestBody = new HashMap<>();
		requestBody.put("email", testEmail);
		requestBody.put("notes", testNotes);

		webTestClient
			.post()
			.uri("/animals/1/adoption-requests")
			.body(BodyInserters.fromValue(requestBody))
			.exchange()
			.expectStatus().isCreated();

		webTestClient
			.get()
			.uri("/animals")
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$[0].id").isEqualTo(1)
			.jsonPath("$[0].name").isEqualTo("Chocobo")
			.jsonPath("$[0].adoptionRequests.length()").isEqualTo(4)
			.jsonPath("$[0].adoptionRequests[*].adopterName").value(hasItem("dummy"))
			.jsonPath("$[0].adoptionRequests[*].email").value(hasItem(testEmail))
			.jsonPath("$[0].adoptionRequests[*].notes").value(hasItem(testNotes));
	}
}
