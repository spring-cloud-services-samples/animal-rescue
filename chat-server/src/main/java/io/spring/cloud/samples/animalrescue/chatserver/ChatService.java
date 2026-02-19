package io.spring.cloud.samples.animalrescue.chatserver;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class ChatService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChatService.class);

	private static final String SYSTEM_PROMPT = """
			You are a friendly assistant for the Animal Rescue center.
			You help potential adopters find animals and complete adoptions.

			CAPABILITIES:
			- Use getAvailableAnimals to look up animals when users ask about them.
			- Use adoptAnimal to submit adoption requests when users want to adopt.

			ADOPTION FLOW:
			1. When a user expresses intent to adopt (e.g. "I want to adopt Chocobo"),
			   first confirm the animal name and look up its ID using getAvailableAnimals.
			2. Ask for their contact email if not already provided.
			3. Ask if they'd like to add any notes (optional).
			4. Call adoptAnimal with the gathered information.
			5. Report the result -- success or error -- clearly.

			IMPORTANT:
			- Anyone can browse and ask about available animals, even without signing in.
			  Always use getAvailableAnimals freely regardless of login status.
			- Only the adoptAnimal action requires authentication. If the user is not
			  logged in (you'll be told in the user context) and wants to adopt, tell
			  them they need to sign in first using the button in the top-right corner.
			- Never fabricate animal IDs. Always look up the real ID from getAvailableAnimals.
			- Be conversational and friendly. Use the animal's name, not just its ID.

			When presenting a list of animals, format them as an HTML table
			with columns: Name, Description, and Pending Adoptions.
			Use <table>, <thead>, <tbody>, <tr>, <th>, and <td> tags.
			Output the HTML tags directly inline in your response — do NOT wrap
			them in markdown code fences (no triple backticks, no ```html blocks).
			Truncate descriptions to at most 8-10 words so the table stays compact.
			For example: "Playful orange tabby, loves to nap" instead of a full paragraph.

			Important guidelines:
			- Only mention animals from the provided data. Never invent animals.
			- The animal descriptions contain hints about personality, energy level,
			  and temperament. Use these to match user preferences.
			- The avatarUrl often contains breed information in the URL path.
			- Be conversational and warm. You represent a rescue center that cares
			  about finding the right match between adopters and animals.
			""";

	private final ChatClient chatClient;
	private final WebClient backendClient;

	public ChatService(ChatClient.Builder chatClientBuilder,
			ToolCallbackProvider[] toolCallbackProviders,
			@Value("${animal-rescue.backend-url:http://localhost:8080}") String backendUrl) {
		this.backendClient = WebClient.create(backendUrl);
		this.chatClient = chatClientBuilder
				.defaultSystem(SYSTEM_PROMPT)
				.defaultToolCallbacks(toolCallbackProviders)
				.build();
	}

	public Flux<String> chat(String userMessage, List<ChatMessage> history, String sessionCookie) {
		LOGGER.info("Processing chat message: {}", userMessage);

		return resolveUsername(sessionCookie)
				.defaultIfEmpty("")
				.flatMapMany(username -> {
					List<Message> messages = buildMessages(history);
					String enrichedMessage = buildEnrichedMessage(userMessage, username);
					return Mono.fromCallable(() -> chatClient.prompt()
							.messages(messages)
							.user(enrichedMessage)
							.call()
							.content())
							.subscribeOn(Schedulers.boundedElastic())
							.flux();
				});
	}

	private String buildEnrichedMessage(String userMessage, String username) {
		StringBuilder sb = new StringBuilder();
		if (username != null && !username.isEmpty()) {
			sb.append("[User context: logged in as \"").append(username).append("\"]\n\n");
		}
		else {
			sb.append("[User context: not logged in]\n\n");
		}
		sb.append(userMessage);
		return sb.toString();
	}

	private Mono<String> resolveUsername(String sessionCookie) {
		if (sessionCookie == null || sessionCookie.isEmpty()) {
			return Mono.just("");
		}
		return backendClient.get()
				.uri("/whoami")
				.cookie("SESSION", sessionCookie)
				.retrieve()
				.bodyToMono(String.class)
				.doOnNext(name -> LOGGER.debug("Resolved username from session: {}", name))
				.onErrorResume(e -> {
					LOGGER.debug("Could not resolve username from session cookie: {}", e.getMessage());
					return Mono.just("");
				});
	}

	private List<Message> buildMessages(List<ChatMessage> history) {
		if (history == null || history.isEmpty()) {
			return List.of();
		}

		List<Message> messages = new ArrayList<>();
		for (ChatMessage msg : history) {
			if ("user".equals(msg.role())) {
				messages.add(new UserMessage(msg.content()));
			}
			else if ("assistant".equals(msg.role())) {
				messages.add(new AssistantMessage(msg.content()));
			}
		}
		return messages;
	}

}
