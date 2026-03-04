package io.spring.cloud.samples.animalrescue.backend.security;

import io.pivotal.cfenv.core.CfEnv;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class SecurityControllerTest {

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
	private CfEnv cfEnv;

	@Test
	@WithMockUser(username = "test-user", authorities = {"adoption.request"})
	void getUserName() {
		webTestClient
			.get()
			.uri("/whoami")
			.exchange()
			.expectStatus().isOk()
			.expectBody(String.class)
			.isEqualTo("test-user");
	}
}
