package io.spring.cloud.samples.animalrescue.backend;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import io.pivotal.cfenv.core.CfEnv;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class AnimalControllerTest {

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private AdoptionRequestRepository adoptionRequestRepository;

	@Autowired
	private AnimalRepository animalRepository;

	@MockBean(answer = Answers.RETURNS_DEEP_STUBS)
	private CfEnv cfEnv;

	private long currentAdoptionRequestCountForAnimalId1;

	@BeforeEach
	void setUp() {
		currentAdoptionRequestCountForAnimalId1 = getAdoptionRequestCountForAnimalId1();
	}

	private int getAdoptionRequestCountForAnimalId1() {
		return animalRepository.findById(1L).get().getAdoptionRequests().size();
	}

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
			.jsonPath("$[0].adoptionRequests.length()").isEqualTo(currentAdoptionRequestCountForAnimalId1)
			.jsonPath("$[0].adoptionRequests[0].adopterName").isNotEmpty()
			.jsonPath("$[0].adoptionRequests[0].email").isNotEmpty()
			.jsonPath("$[0].adoptionRequests[0].notes").isNotEmpty();
	}

	@Test
	@WithMockUser(username = "test-user", authorities = { "adoption.request" })
	void submitAdoptionRequest() {
		String testEmail = "a@email.com";
		String testNotes = "Yaaas!";

		adopt(testEmail, testNotes);

		webTestClient
			.get()
			.uri("/animals")
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$[0].id").isEqualTo(1)
			.jsonPath("$[0].name").isEqualTo("Chocobo")
			.jsonPath("$[0].adoptionRequests.length()").isEqualTo(currentAdoptionRequestCountForAnimalId1 + 1)
			.jsonPath("$[0].adoptionRequests[*].adopterName").value(hasItem("dummy"))
			.jsonPath("$[0].adoptionRequests[*].email").value(hasItem(testEmail))
			.jsonPath("$[0].adoptionRequests[*].notes").value(hasItem(testNotes));
	}

	private void adopt(String testEmail, String testNotes) {
		Map<String, String> requestBody = new HashMap<>();
		requestBody.put("email", testEmail);
		requestBody.put("notes", testNotes);

		webTestClient
			.post()
			.uri("/animals/1/adoption-requests")
			.body(BodyInserters.fromValue(requestBody))
			.exchange()
			.expectStatus().isCreated();
	}

	@Test
	@WithMockUser(username = "test-user", authorities = { "adoption.request" })
	void editAdoptionRequest() {
		String testEmail = "a@email.com";
		String testNotes = "Yaaas!";

		Map<String, String> requestBody = new HashMap<>();
		requestBody.put("email", testEmail);
		requestBody.put("notes", testNotes);

		webTestClient
			.put()
			.uri("/animals/1/adoption-requests/2")
			.body(BodyInserters.fromValue(requestBody))
			.exchange()
			.expectStatus().isOk();

		Optional<AdoptionRequest> modified = adoptionRequestRepository.findById(2L);
		assertThat(modified).isPresent();
		assertThat(modified.get().getEmail()).isEqualTo(testEmail);
		assertThat(modified.get().getNotes()).isEqualTo(testNotes);
		assertThat(modified.get().getAdopterName()).isEqualTo("Gareth");
		assertThat(getAdoptionRequestCountForAnimalId1()).isEqualTo(currentAdoptionRequestCountForAnimalId1);
	}

	@Test
	@WithMockUser(username = "test-user", authorities = { "adoption.request" })
	void deleteAdoptionRequest() {
		webTestClient
			.delete()
			.uri("/animals/1/adoption-requests/1")
			.exchange()
			.expectStatus().isOk();

		assertThat(adoptionRequestRepository.findById(1L)).isNotPresent();
		assertThat(getAdoptionRequestCountForAnimalId1()).isEqualTo(currentAdoptionRequestCountForAnimalId1 - 1);
	}
}
